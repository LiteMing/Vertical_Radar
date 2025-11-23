package com.example.examplemod.radar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class VerticalRadarRenderer implements IGuiOverlay {

    public static final VerticalRadarRenderer INSTANCE = new VerticalRadarRenderer();
    
    // Configuration constants (could be moved to a config file)
    private static final int RADAR_SIZE = 200; // Size of the radar area in pixels
    private static final float RADAR_SCALE = 10.0f; // Pixels per block
    private static final float MAX_DISTANCE = 50.0f; // Max distance to show entities
    private static final int DOT_SIZE = 4; // Size of the entity dot

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        // Radar center position (e.g., center of screen or offset)
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Iterate over all entities
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity)) continue; // Filter non-living entities if desired

            // 1. Get relative position
            Vec3 playerPos = player.getPosition(partialTick);
            Vec3 entityPos = entity.getPosition(partialTick);
            Vec3 relativePos = entityPos.subtract(playerPos);
            
            double dx = relativePos.x;
            double dy = relativePos.y;
            double dz = relativePos.z;

            // 2. Rotate based on player Yaw (around Y axis)
            // Minecraft Yaw: 0 = South, 90 = West, 180 = North, 270 = East
            // Standard math usually expects 0 = East.
            // We need to rotate by -yaw.
            float yaw = player.getViewYRot(partialTick);
            double yawRad = Math.toRadians(-yaw); // Negative because MC yaw rotation is clockwise? Check standard
            
            // Standard 2D rotation:
            // x' = x cos(theta) - z sin(theta)
            // z' = x sin(theta) + z cos(theta)
            // In MC, Yaw is rotation around Y axis.
            // Let's use the standard formula for rotating a vector (dx, dy, dz) by -yaw around Y.
            // For MC, Yaw increases clockwise. To bring world to view, we rotate counter-clockwise (negative of yaw).
            // Actually, let's just use the standard formula:
            // x_view = dx * cos(yaw) + dz * sin(yaw)  <-- Wait, checking math
            // Let's stick to the formula derived in design:
            // x' = x * cos(-yaw) - z * sin(-yaw)
            // z' = x * sin(-yaw) + z * cos(-yaw)
            // Note: Math.sin/cos take radians.
            
            // Correction: MC Yaw 0 is +Z (South). +90 is -X (West).
            // So +Z is forward? No, usually LookVec is calculated from Yaw/Pitch.
            // Let's use simple logic: Rotate the world so player looks along +Z axis.
            
            double cosYaw = Math.cos(yawRad);
            double sinYaw = Math.sin(yawRad);

            // Rotate around Y axis
            double x1 = dx * cosYaw - dz * sinYaw;
            double z1 = dx * sinYaw + dz * cosYaw;
            double y1 = dy;

            // 3. Rotate based on player Pitch (around X axis)
            float pitch = player.getViewXRot(partialTick);
            double pitchRad = Math.toRadians(-pitch); // Negative to bring view to horizontal

            double cosPitch = Math.cos(pitchRad);
            double sinPitch = Math.sin(pitchRad);

            // Rotate around X axis
            // y' = y * cos(theta) - z * sin(theta)
            // z' = y * sin(theta) + z * cos(theta)
            
            double y2 = y1 * cosPitch - z1 * sinPitch;
            double z2 = y1 * sinPitch + z1 * cosPitch;
            double x2 = x1;

            // Now:
            // x2 is Horizontal offset (Right/Left)
            // y2 is Vertical offset (Up/Down)
            // z2 is Depth (Forward/Backward) - Positive should be forward

            // Filter out entities behind the player
            if (z2 < 0) continue;
            
            // Filter by max distance
            double dist = Math.sqrt(x2*x2 + y2*y2 + z2*z2);
            if (dist > MAX_DISTANCE) continue;

            // 4. Project to screen
            // x2 is horizontal offset relative to view center
            // y2 is vertical offset relative to view center
            
            // Scale logic: RADAR_SCALE pixels per block of offset
            float screenX = (float) (centerX - x2 * RADAR_SCALE); // Minus x2? Need to check coordinate system.
            // Usually: +X is Right. If entity is to the right, x2 should be positive.
            // Screen X increases to right. So centerX + x2.
            // Let's verify rotation: 
            // If looking South (Yaw 0), Entity at West (X-10). dx=-10, dz=0. 
            // yawRad=0. x1 = -10. 
            // If looking West (Yaw 90). Entity at West (Relative X=0, Z=something).
            // This suggests my initial rotation formula needs care.
            
            // Let's rely on standard practice:
            // x_screen = x2
            // y_screen = y2
            
            // Correct mapping:
            // If x2 is positive (right of player), we want it to right of center. -> centerX + x2 * scale
            // If y2 is positive (above player), we want it above center. -> centerY - y2 * scale (Screen Y is down)
            
            float renderX = (float) (centerX - x2 * RADAR_SCALE); // Inverted X? Let's try minus first. 
            // Wait, if I look South (Yaw 0), East is -X in MC? No.
            // South is +Z. East is +X.
            // If I look South, Entity at East (+10, 0, 0).
            // Yaw=0. cos=1, sin=0.
            // x1 = 10*1 - 0 = 10.
            // So entity is at x1=10. This is to my Left? 
            // If I look South, East is to my Left.
            // So +x1 means Left. 
            // Screen Left is -X.
            // So ScreenX = Center - x1 * Scale. This matches.
            
            float renderY = (float) (centerY - y2 * RADAR_SCALE); // Screen Y increases downwards. Positive y2 (up) -> smaller Screen Y.

            // 5. Calculate Color/Alpha
            int color = 0xFFFFFFFF; // White default
            if (entity instanceof Enemy) {
                color = 0xFFFF0000; // Red for enemies
            } else if (entity instanceof Player) {
                color = 0xFF00FF00; // Green for players
            } else {
                color = 0xFFFFFF00; // Yellow for others
            }

            // Alpha based on distance
            float alphaFactor = 1.0f - (float)(dist / MAX_DISTANCE);
            if (alphaFactor < 0.1f) alphaFactor = 0.1f;
            int alpha = (int) (alphaFactor * 255);
            
            // Combine alpha into color
            // Ensure base color has 00 alpha before ORing, or just overwrite
            // Format: ARGB
            color = (color & 0x00FFFFFF) | (alpha << 24);

            // 6. Render
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            
            // Draw a simple square dot
            // Center the dot
            guiGraphics.fill((int)renderX - DOT_SIZE/2, (int)renderY - DOT_SIZE/2, 
                             (int)renderX + DOT_SIZE/2, (int)renderY + DOT_SIZE/2, 
                             color);
            
            RenderSystem.disableBlend();
        }
    }
}
