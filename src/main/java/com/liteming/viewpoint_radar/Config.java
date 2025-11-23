package com.liteming.viewpoint_radar;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ViewpointRadarMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ========== Radar General Settings ==========
    static {
        BUILDER.comment("General Radar Settings").push("general");
    }

    private static final ForgeConfigSpec.BooleanValue RADAR_ENABLED = BUILDER
            .comment("Enable or disable the vertical radar overlay")
            .define("radarEnabled", true);

    private static final ForgeConfigSpec.ConfigValue<String> RADAR_TOGGLE_KEY = BUILDER
            .comment("Key binding to toggle radar on/off (e.g., 'R', 'V', 'NUMPAD1')")
            .define("radarToggleKey", "R");

    static {
        BUILDER.pop();
    }

    // ========== Entity Type Toggles ==========
    static {
        BUILDER.comment("Entity Type Filters").push("entities");
    }

    private static final ForgeConfigSpec.BooleanValue SHOW_ENEMIES = BUILDER
            .comment("Show hostile mobs (zombies, skeletons, creepers, etc.) on the radar")
            .define("showEnemies", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_PLAYERS = BUILDER
            .comment("Show other players on the radar")
            .define("showPlayers", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_PROJECTILES = BUILDER
            .comment("Show projectiles (arrows, fireballs, tridents, etc.) on the radar")
            .define("showProjectiles", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_OTHER_LIVING = BUILDER
            .comment("Show other living entities (animals, villagers, etc.) on the radar")
            .define("showOtherLiving", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_ALL_COLLIDABLE = BUILDER
            .comment("Show ALL entities with collision boxes (includes modded entities, minecarts, boats, etc.)")
            .define("showAllCollidable", false);

    static {
        BUILDER.pop();
    }

    // ========== Radar Display Settings ==========
    static {
        BUILDER.comment("Display Settings").push("display");
    }

    private static final ForgeConfigSpec.IntValue RADAR_SCALE = BUILDER
            .comment("Pixels per block for radar display (higher = more zoomed in)")
            .defineInRange("radarScale", 15, 5, 50);

    private static final ForgeConfigSpec.IntValue DOT_SIZE = BUILDER
            .comment("Size of the entity dots/circles on radar (diameter in pixels)")
            .defineInRange("dotSize", 6, 2, 20);

    private static final ForgeConfigSpec.DoubleValue OUTLINE_THICKNESS = BUILDER
            .comment("Thickness of the circle outline in pixels (supports decimal values)")
            .defineInRange("outlineThickness", 1.5, 0.5, 5.0);

    static {
        BUILDER.pop();
    }

    // ========== Detection Settings ==========
    static {
        BUILDER.comment("Detection Range Settings").push("detection");
    }

    private static final ForgeConfigSpec.IntValue ENTITY_SCAN_DISTANCE = BUILDER
            .comment("Distance in blocks to scan for entities (affects performance)")
            .defineInRange("entityScanDistance", 64, 16, 256);

    private static final ForgeConfigSpec.IntValue MAX_DISPLAY_DISTANCE = BUILDER
            .comment("Maximum distance in blocks to display entities on radar (must be <= scan distance)")
            .defineInRange("maxDisplayDistance", 50, 10, 200);

    static {
        BUILDER.pop();
    }

    // Build the config spec
    static final ForgeConfigSpec SPEC = BUILDER.build();

    // ========== Public Static Fields for Easy Access ==========
    public static boolean radarEnabled;
    public static String radarToggleKey;
    public static boolean showEnemies;
    public static boolean showPlayers;
    public static boolean showProjectiles;
    public static boolean showOtherLiving;
    public static boolean showAllCollidable;
    public static int radarScale;
    public static int dotSize;
    public static double outlineThickness;
    public static int entityScanDistance;
    public static int maxDisplayDistance;

    // Runtime toggle (not saved to config)
    public static boolean radarRuntimeEnabled = true;

    // ========== Config Reload Handler ==========
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Load all config values when config is loaded or reloaded
        radarEnabled = RADAR_ENABLED.get();
        radarToggleKey = RADAR_TOGGLE_KEY.get();
        showEnemies = SHOW_ENEMIES.get();
        showPlayers = SHOW_PLAYERS.get();
        showProjectiles = SHOW_PROJECTILES.get();
        showOtherLiving = SHOW_OTHER_LIVING.get();
        showAllCollidable = SHOW_ALL_COLLIDABLE.get();
        radarScale = RADAR_SCALE.get();
        dotSize = DOT_SIZE.get();
        outlineThickness = OUTLINE_THICKNESS.get();
        entityScanDistance = ENTITY_SCAN_DISTANCE.get();
        maxDisplayDistance = MAX_DISPLAY_DISTANCE.get();

        // Validate that maxDisplayDistance <= entityScanDistance
        if (maxDisplayDistance > entityScanDistance) {
            maxDisplayDistance = entityScanDistance;
        }
    }

    // ========== Toggle Methods ==========
    public static void toggleRadar() {
        radarRuntimeEnabled = !radarRuntimeEnabled;
    }

    public static boolean isRadarActive() {
        return radarEnabled && radarRuntimeEnabled;
    }
}