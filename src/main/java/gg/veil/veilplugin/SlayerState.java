package gg.veil.veilplugin;

import lombok.Data;

@Data
public class SlayerState
{
    public int    taskId;
    public String taskName;    // resolved by Veil app from taskId
    public int    remaining;   // kills left
    public int    initialAmount;
    public int    locationId;
    public int    points;
    public int    streak;

    public boolean hasTask() { return remaining > 0; }
    public int percentComplete()
    {
        if (initialAmount <= 0) return 0;
        return (int)((1.0 - (double)remaining / initialAmount) * 100);
    }
}
