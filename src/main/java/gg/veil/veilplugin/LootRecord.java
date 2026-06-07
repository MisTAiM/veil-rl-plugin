package gg.veil.veilplugin;

import lombok.Data;
import java.util.List;

@Data
public class LootRecord
{
    public long        timestamp;
    public int         totalGp;
    public String      rsn;
    public List<LootItem> items;

    @Data
    public static class LootItem
    {
        public int    itemId;
        public String itemName;
        public int    quantity;
        public int    priceEach;
        public int    totalValue;

        public LootItem(int itemId, String name, int qty, int price, int total)
        {
            this.itemId     = itemId;
            this.itemName   = name;
            this.quantity   = qty;
            this.priceEach  = price;
            this.totalValue = total;
        }
    }
}
