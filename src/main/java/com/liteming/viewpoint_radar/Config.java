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
            .defineInRange("radarScale", 5, 1, 50);

    private static final ForgeConfigSpec.IntValue DOT_SIZE = BUILDER
            .comment("Size of the entity dots/circles on radar (diameter in pixels)")
            .defineInRange("dotSize", 15, 1, 50);

    private static final ForgeConfigSpec.DoubleValue OUTLINE_THICKNESS = BUILDER
            .comment("Thickness of the circle outline in pixels (supports decimal values)")
            .defineInRange("outlineThickness", 3, 0.5, 10.0);

    private static final ForgeConfigSpec.DoubleValue RADAR_BACKGROUND_ALPHA = BUILDER
            .comment("Background transparency (0.0 = fully transparent, 1.0 = fully opaque)")
            .defineInRange("radarBackgroundAlpha", 0.25, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue RADAR_WIDTH_SCALE = BUILDER
            .comment("Radar width as a fraction of screen width (e.g., 0.67 = 2/3 of screen)")
            .defineInRange("radarWidthScale", 0.67, 0.2, 1.0);

    private static final ForgeConfigSpec.DoubleValue RADAR_HEIGHT_SCALE = BUILDER
            .comment("Radar height as a fraction of screen height (e.g., 0.67 = 2/3 of screen)")
            .defineInRange("radarHeightScale", 0.67, 0.2, 1.0);

    private static final ForgeConfigSpec.DoubleValue RADAR_POSITION_X = BUILDER
            .comment("Radar horizontal position (0.0 = left edge, 0.5 = center, 1.0 = right edge)")
            .defineInRange("radarPositionX", 0.5, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue RADAR_POSITION_Y = BUILDER
            .comment("Radar vertical position (0.0 = top edge, 1.0 = bottom edge)")
            .defineInRange("radarPositionY", 1.0, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    // ========== Projectile Progress Settings ==========
    static {
        BUILDER.comment("Projectile Progress Indicator Settings").push("projectile_progress");
    }

    private static final ForgeConfigSpec.BooleanValue SHOW_PROJECTILE_PROGRESS = BUILDER
            .comment("Show progress indicator inside projectile circles",
                    "When enabled, a semi-transparent filled circle grows as projectiles approach the player's view plane")
            .define("showProjectileProgress", true);

    private static final ForgeConfigSpec.DoubleValue PROGRESS_CIRCLE_ALPHA = BUILDER
            .comment("Opacity of the progress circle (0.0 = fully transparent, 1.0 = fully opaque)")
            .defineInRange("progressCircleAlpha", 0.4, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue PROGRESS_MIN_RADIUS_PERCENT = BUILDER
            .comment("Minimum radius of progress circle as a percentage of the outer circle",
                    "0.05 = 5% (almost invisible when far), 0.1 = 10%, etc.")
            .defineInRange("progressMinRadiusPercent", 0.01, 0.01, 0.5);



    static {
        BUILDER.pop();
    }

    // ========== Detection Settings ==========
    static {
        BUILDER.comment("Detection Range Settings").push("detection");
    }

    private static final ForgeConfigSpec.IntValue ENTITY_SCAN_DISTANCE = BUILDER
            .comment("Distance in blocks to scan for entities (affects performance)")
            .defineInRange("entityScanDistance", 32, 16, 256);

    private static final ForgeConfigSpec.IntValue MAX_DISPLAY_DISTANCE = BUILDER
            .comment("Maximum distance in blocks to display entities on radar (must be <= scan distance)")
            .defineInRange("maxDisplayDistance", 32, 10, 200);

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

    // Background and position settings
    public static double radarBackgroundAlpha;
    public static double radarWidthScale;
    public static double radarHeightScale;
    public static double radarPositionX;
    public static double radarPositionY;

    // NEW: Projectile progress settings
    public static boolean showProjectileProgress;
    public static double progressCircleAlpha;
    public static double progressMinRadiusPercent;

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

// Background and position settings
        radarBackgroundAlpha = RADAR_BACKGROUND_ALPHA.get();
        radarWidthScale = RADAR_WIDTH_SCALE.get();
        radarHeightScale = RADAR_HEIGHT_SCALE.get();
        radarPositionX = RADAR_POSITION_X.get();
        radarPositionY = RADAR_POSITION_Y.get();

// NEW: Load projectile progress settings
        showProjectileProgress = SHOW_PROJECTILE_PROGRESS.get();
        progressCircleAlpha = PROGRESS_CIRCLE_ALPHA.get();
        progressMinRadiusPercent = PROGRESS_MIN_RADIUS_PERCENT.get();


// Validate that maxDisplayDistance <= entityScanDistance
        if (maxDisplayDistance > entityScanDistance) {
            maxDisplayDistance = entityScanDistance;
        }
    }

    // ========== Toggle Methods ==========

    public static boolean isRadarActive() {
        return radarEnabled && radarRuntimeEnabled;
    }
}