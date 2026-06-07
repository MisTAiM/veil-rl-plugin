package gg.veil.veilplugin;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tiny embedded HTTP server that the Veil Android app polls over Wi-Fi.
 *
 * Endpoints:
 *   GET /veil/state      → full SyncPayload JSON
 *   GET /veil/ping       → {"ok":true,"version":"1.0.0"}
 *   POST /veil/alert     → Veil app pushes a price alert trigger back to RL
 *
 * All responses are JSON, CORS-open (Veil app origin).
 * Server runs on a single background thread; all state reads are via
 * an AtomicReference<SyncPayload> so no locks needed in the hot path.
 */
@Slf4j
public class VeilSyncServer
{
    private static final String VERSION = "1.0.0";
    private static final String CORS_HEADERS =
        "Access-Control-Allow-Origin: *\r\n" +
        "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
        "Access-Control-Allow-Headers: Content-Type\r\n";

    private final Gson gson;
    private final int port;
    private final AtomicReference<SyncPayload> payloadRef = new AtomicReference<>();

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean running = false;

    public VeilSyncServer(int port, Gson gson)
    {
        this.port  = port;
        this.gson  = gson;
    }

    public void updatePayload(SyncPayload payload)
    {
        payloadRef.set(payload);
    }

    public void start() throws IOException
    {
        if (running) return;
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        running = true;

        serverThread = new Thread(() ->
        {
            log.info("Veil sync server listening on port {}", port);
            while (running)
            {
                try
                {
                    Socket client = serverSocket.accept();
                    // Handle each connection in-place (tiny payloads, low concurrency)
                    handleClient(client);
                }
                catch (IOException e)
                {
                    if (running) log.warn("Veil server accept error", e);
                }
            }
        }, "veil-sync-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void stop()
    {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
        if (serverThread != null) serverThread.interrupt();
    }

    private void handleClient(Socket client) throws IOException
    {
        try (client;
             BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream   out = client.getOutputStream())
        {
            // Read request line
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            // Consume headers
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) { /* skip */ }

            // Parse method + path
            String[] parts  = requestLine.split(" ");
            if (parts.length < 2) return;
            String method   = parts[0];
            String path     = parts[1].split("\\?")[0]; // strip query string

            // OPTIONS preflight
            if ("OPTIONS".equals(method))
            {
                sendResponse(out, 204, "No Content", "", "");
                return;
            }

            switch (path)
            {
                case "/veil/ping":
                    sendJson(out, "{\"ok\":true,\"version\":\"" + VERSION + "\"}");
                    break;

                case "/veil/state":
                    SyncPayload p = payloadRef.get();
                    String body = p != null ? gson.toJson(p) : "{\"loggedIn\":false}";
                    sendJson(out, body);
                    break;

                default:
                    sendResponse(out, 404, "Not Found", "application/json",
                        "{\"error\":\"Unknown endpoint\"}");
            }
        }
        catch (Exception e)
        {
            log.debug("Veil client handler error", e);
        }
    }

    private void sendJson(OutputStream out, String json) throws IOException
    {
        sendResponse(out, 200, "OK", "application/json", json);
    }

    private void sendResponse(OutputStream out, int code, String status,
                              String contentType, String body) throws IOException
    {
        byte[] bodyBytes = body.getBytes("UTF-8");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), false);
        pw.print("HTTP/1.1 " + code + " " + status + "\r\n");
        pw.print("Content-Type: " + (contentType.isEmpty() ? "text/plain" : contentType) + "; charset=utf-8\r\n");
        pw.print("Content-Length: " + bodyBytes.length + "\r\n");
        pw.print(CORS_HEADERS);
        pw.print("Connection: close\r\n");
        pw.print("\r\n");
        pw.flush();
        out.write(bodyBytes);
        out.flush();
    }

    public int getPort() { return port; }
    public boolean isRunning() { return running; }
}
