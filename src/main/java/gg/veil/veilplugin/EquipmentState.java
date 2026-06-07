package gg.veil.veilplugin;

import lombok.Data;
import java.util.List;

@Data
public class EquipmentState
{
    public int  totalValue;       // GE value of all worn items
    public int  protectedValue;   // value of top 3 (or 4 with protect item)
    public int  atRiskValue;      // totalValue - protectedValue
    public List<EquipSlot> slots;

    @Data
    public static class EquipSlot
    {
        public int    itemId;
        public String itemName;
        public int    quantity;
        public int    geValue;
        public int    slot; // 0=helm,1=cape,2=ammy,3=weapon,4=body,5=shield,6=legs,7=gloves,8=boots,9=ring,10=ammo
    }
}
