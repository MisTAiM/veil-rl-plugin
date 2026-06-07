package gg.veil.veilplugin;

import net.runelite.client.config.*;

@ConfigGroup("veil")
public interface VeilConfig extends Config
{
    @ConfigSection(name="Sync Server", description="Local HTTP server for Veil mobile", position=0)
    String syncSection = "sync";

    @ConfigItem(keyName="serverPort", name="Port", description="Port the sync server listens on", position=0, section=syncSection)
    default int serverPort() { return 7337; }

    @ConfigItem(keyName="serverEnabled", name="Enable Sync", description="Enable local HTTP sync server", position=1, section=syncSection)
    default boolean serverEnabled() { return true; }

    @ConfigSection(name="Tracking", description="What to track and display", position=1)
    String trackSection = "track";

    @ConfigItem(keyName="showOverlay", name="GE Overlay", description="Show overlay when GE is open", position=0, section=trackSection)
    default boolean showOverlay() { return true; }

    @ConfigItem(keyName="trackLoot", name="Track Loot", description="Track loot via inventory diff", position=1, section=trackSection)
    default boolean trackLoot() { return true; }

    @ConfigItem(keyName="trackXp", name="Track XP", description="Track XP gains via StatChanged", position=2, section=trackSection)
    default boolean trackXp() { return true; }

    @ConfigSection(name="Alerts", description="Notifications", position=2)
    String alertSection = "alerts";

    @ConfigItem(keyName="alertOnFill", name="Alert on Fill", description="Notify when offer completes", position=0, section=alertSection)
    default boolean alertOnFill() { return true; }

    @ConfigItem(keyName="alertThresholdGp", name="Min GP Alert", description="Only alert above this profit/loot value", position=1, section=alertSection)
    @Range(min=0, max=10_000_000)
    default int alertThresholdGp() { return 50_000; }
}
