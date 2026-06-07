package gg.veil.veilplugin;

import net.runelite.client.config.*;

@ConfigGroup("veil")
public interface VeilConfig extends Config
{
    // ── OVERLAY ───────────────────────────────────────────────
    @ConfigSection(name="Overlay", description="Control what shows on the in-game overlay", position=0)
    String overlaySection = "overlay";

    @ConfigItem(keyName="showOverlay",      name="Show Overlay",        description="Show the Veil overlay in-game",                    position=0,  section=overlaySection)
    default boolean showOverlay()       { return true; }

    @ConfigItem(keyName="showCoins",        name="Show Coin Stack",     description="Show your live coin count",                        position=1,  section=overlaySection)
    default boolean showCoins()         { return true; }

    @ConfigItem(keyName="showGpToday",      name="Show GP Today",       description="Show session profit + loot total",                 position=2,  section=overlaySection)
    default boolean showGpToday()       { return true; }

    @ConfigItem(keyName="showXp",           name="Show XP/hr",          description="Show XP gained and rate",                         position=3,  section=overlaySection)
    default boolean showXp()            { return true; }

    @ConfigItem(keyName="showCharges",      name="Show Weapon Charges", description="Show blowpipe/trident/etc charge counts",          position=4,  section=overlaySection)
    default boolean showCharges()       { return true; }

    @ConfigItem(keyName="showSlayer",       name="Show Slayer",         description="Show current slayer task and KC remaining",        position=5,  section=overlaySection)
    default boolean showSlayer()        { return true; }

    @ConfigItem(keyName="showDeathRisk",    name="Show Death Risk",     description="Show GP at risk on death",                        position=6,  section=overlaySection)
    default boolean showDeathRisk()     { return true; }

    @ConfigItem(keyName="showGeSlots",      name="Show GE Slots",       description="Show active GE offer slots",                      position=7,  section=overlaySection)
    default boolean showGeSlots()       { return true; }

    @ConfigItem(keyName="showTopFlip",      name="Show Top Flip",       description="Show best current flip opportunity",               position=8,  section=overlaySection)
    default boolean showTopFlip()       { return true; }

    @ConfigItem(keyName="showPrayerSpec",   name="Show Prayer/Spec",    description="Show prayer points and special attack %",         position=9,  section=overlaySection)
    default boolean showPrayerSpec()    { return false; }

    @ConfigItem(keyName="deathRiskThreshold", name="Death Risk Alert GP", description="Highlight death risk red above this value",    position=10, section=overlaySection)
    @Range(min=0, max=50_000_000)
    default int deathRiskThreshold()   { return 5_000_000; }

    // ── FLIPS ─────────────────────────────────────────────────
    @ConfigSection(name="Flip Settings", description="Filter and tune the flip finder", position=1)
    String flipSection = "flips";

    @ConfigItem(keyName="minNetMargin",  name="Min Net Margin",    description="Minimum GP profit per item after tax",               position=0, section=flipSection)
    @Range(min=0, max=500_000)
    default int minNetMargin()         { return 500; }

    @ConfigItem(keyName="maxFillMins",   name="Max Fill Time (min)", description="Maximum minutes to fill a full limit order",       position=1, section=flipSection)
    @Range(min=5, max=240)
    default int maxFillMins()          { return 90; }

    @ConfigItem(keyName="minRoi",        name="Min ROI %",          description="Minimum return on investment percentage",           position=2, section=flipSection)
    @Range(min=0, max=20)
    default int minRoi()               { return 0; }

    @ConfigItem(keyName="membersFilter", name="Members Items Only",  description="Show only members-only tradeable items",           position=3, section=flipSection)
    default boolean membersFilter()    { return false; }

    @ConfigItem(keyName="hideDGrade",    name="Hide D-Grade Items",  description="Hide items graded D (low GP/hr)",                  position=4, section=flipSection)
    default boolean hideDGrade()       { return true; }

    @ConfigItem(keyName="enterOnly",     name="ENTER Signal Only",   description="Only show items with ENTER signal",                position=5, section=flipSection)
    default boolean enterOnly()        { return false; }

    // ── ALERTS ────────────────────────────────────────────────
    @ConfigSection(name="Alerts", description="Desktop notification settings", position=2)
    String alertSection = "alerts";

    @ConfigItem(keyName="alertOnFill",      name="Alert on GE Fill",      description="Notify when an offer completes",              position=0, section=alertSection)
    default boolean alertOnFill()       { return true; }

    @ConfigItem(keyName="alertThresholdGp", name="Min GP for Alert",       description="Only alert when profit/loot exceeds this",   position=1, section=alertSection)
    @Range(min=0, max=10_000_000)
    default int alertThresholdGp()      { return 50_000; }

    @ConfigItem(keyName="alertLowCharges",  name="Low Weapon Charges",     description="Notify when weapon charges are running low",  position=2, section=alertSection)
    default boolean alertLowCharges()   { return true; }

    @ConfigItem(keyName="alertSupplyShock", name="Supply Shock Alert",     description="Notify when extreme buy pressure detected",   position=3, section=alertSection)
    default boolean alertSupplyShock()  { return true; }

    @ConfigItem(keyName="alertPriceSpike", name="Price Spike Alert",      description="Notify when item moves >5% in 30 minutes",   position=4, section=alertSection)
    default boolean alertPriceSpike()   { return true; }

    // ── TRACKING ──────────────────────────────────────────────
    @ConfigSection(name="Tracking", description="What data to collect", position=3)
    String trackSection = "track";

    @ConfigItem(keyName="trackLoot",    name="Track Loot",    description="Auto-detect loot via inventory diff",                   position=0, section=trackSection)
    default boolean trackLoot()        { return true; }

    @ConfigItem(keyName="trackXp",      name="Track XP",      description="Track XP gains in real-time via StatChanged",          position=1, section=trackSection)
    default boolean trackXp()          { return true; }

    @ConfigItem(keyName="trackSlayer",  name="Track Slayer",  description="Track slayer task, KC, points, streak",                position=2, section=trackSection)
    default boolean trackSlayer()      { return true; }
}
