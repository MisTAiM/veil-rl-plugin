package gg.veil.veilplugin;

import lombok.Data;

@Data
public class WeaponCharges
{
    // All values are -1 if item not owned / not detectable
    public int blowpipeCharges  = -1;
    public int tridentCharges   = -1;
    public int sangStaffCharges = -1;
    public int scytheCharges    = -1;
    public int tumekensCharges  = -1;

    // Warnings: true if below safe threshold
    public boolean blowpipeLow  = false;
    public boolean tridentLow   = false;
    public boolean sangStaffLow = false;
    public boolean scytheLow    = false;
    public boolean tumekensLow  = false;
}
