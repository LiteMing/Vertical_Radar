package com.liteming.viewpoint_radar.radar;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.render.virtual.ClientDanmakuCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import com.liteming.viewpoint_radar.Config;
import com.liteming.viewpoint_radar.accessor.ClientDanmakuCacheAccessor;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;

public class VerticalRadarRenderer implements IGuiOverlay {

    public static final VerticalRadarRenderer INSTANCE = new VerticalRadarRenderer();

    private static final int CIRCLE_SEGMENTS = 24;
    private static final boolean YOUKAI_LOADED = ModList.get().isLoaded("youkaishomecoming");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        if (!Config.isRadarActive()) return;

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        Vec3 playerPos = player.getPosition(partialTick);

        double scanDistance = Config.entityScanDistance;
        AABB scanBox = new AABB(
                playerPos.x - scanDistance, playerPos.y - scanDistance, playerPos.z - scanDistance,
                playerPos.x + scanDistance, playerPos.y + scanDistance, playerPos.z + scanDistance
        );

        // 准备渲染状态
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        // 渲染普通实体
        for (Entity entity : mc.level.getEntities(null, scanBox)) {
            if (entity == player) continue;
            if (!shouldRenderEntity(entity)) continue;

            renderEntityOnRadar(entity, playerPos, partialTick, centerX, centerY, guiGraphics);
        }

        // 渲染YH弹幕
        if (YOUKAI_LOADED && Config.showProjectiles) {
            renderYHDanmaku(playerPos, partialTick, centerX, centerY, guiGraphics, mc);
        }

        // 恢复渲染状态
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderYHDanmaku(Vec3 playerPos, float partialTick, int centerX, int centerY,
                                 GuiGraphics guiGraphics, Minecraft mc) {
        try {
            ClientDanmakuCache cache = ClientDanmakuCache.get(mc.level);
            if (cache == null) return;

            // 使用 Mixin accessor
            Iterable<SimplifiedProjectile> allDanmaku =
                    ((ClientDanmakuCacheAccessor) cache).viewpointRadar$getAllProjectiles();

            for (SimplifiedProjectile projectile : allDanmaku) {
                if (!projectile.isValid()) continue;

                double lerpX = lerp(partialTick, projectile.xOld, projectile.getX());
                double lerpY = lerp(partialTick, projectile.yOld, projectile.getY());
                double lerpZ = lerp(partialTick, projectile.zOld, projectile.getZ());
                Vec3 projectilePos = new Vec3(lerpX, lerpY, lerpZ);

                double dist = projectilePos.distanceTo(playerPos);
                if (dist > Config.entityScanDistance) continue;

                Vec3 relativePos = projectilePos.subtract(playerPos);
                Vec3 transformedPos = transformToViewSpace(relativePos, partialTick, mc.player);

                if (transformedPos == null) continue;

                double viewDist = Math.sqrt(
                        transformedPos.x * transformedPos.x +
                                transformedPos.y * transformedPos.y +
                                transformedPos.z * transformedPos.z
                );
                if (viewDist > Config.maxDisplayDistance) continue;

                float renderX = (float) (centerX - transformedPos.x * Config.radarScale);
                float renderY = (float) (centerY - transformedPos.y * Config.radarScale);

                int color = 0xFFFF00FF;
                float alphaFactor = 1.0f - (float)(viewDist / Config.maxDisplayDistance);
                if (alphaFactor < 0.15f) alphaFactor = 0.15f;
                int alpha = (int) (alphaFactor * 255);
                color = (color & 0x00FFFFFF) | (alpha << 24);

                renderCircle(guiGraphics, renderX, renderY, Config.dotSize / 2.0f, color);
            }
        } catch (Exception e) {
            e.printStackTrace(); // 调试时打印错误
        }
    }

    private void renderEntityOnRadar(Entity entity, Vec3 playerPos, float partialTick,
                                     int centerX, int centerY, GuiGraphics guiGraphics) {
        Vec3 entityPos = entity.getPosition(partialTick);
        Vec3 relativePos = entityPos.subtract(playerPos);

        Vec3 transformedPos = transformToViewSpace(relativePos, partialTick,
                Minecraft.getInstance().player);

        if (transformedPos == null) return;

        double dist = Math.sqrt(
                transformedPos.x * transformedPos.x +
                        transformedPos.y * transformedPos.y +
                        transformedPos.z * transformedPos.z
        );
        if (dist > Config.maxDisplayDistance) return;

        float renderX = (float) (centerX - transformedPos.x * Config.radarScale);
        float renderY = (float) (centerY - transformedPos.y * Config.radarScale);

        int color = getEntityColor(entity);

        float alphaFactor = 1.0f - (float)(dist / Config.maxDisplayDistance);
        if (alphaFactor < 0.15f) alphaFactor = 0.15f;
        int alpha = (int) (alphaFactor * 255);
        color = (color & 0x00FFFFFF) | (alpha << 24);

        renderCircle(guiGraphics, renderX, renderY, Config.dotSize / 2.0f, color);
    }

    /**
     * 使用多层圆环模拟可变线宽的空心圆
     */
    private void renderCircle(GuiGraphics guiGraphics, float x, float y, float radius, int color) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = pose.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // 计算线宽对应的层数
        int layers = Math.max(1, (int) Math.ceil(Config.outlineThickness));
        float radiusStep = (float) (Config.outlineThickness / layers);

        // 绘制多层圆环来模拟线宽
        for (int layer = 0; layer < layers; layer++) {
            float currentRadius = radius - (layer * radiusStep);
            if (currentRadius <= 0) break;

            buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                double angle = 2.0 * Math.PI * i / CIRCLE_SEGMENTS;
                float circleX = x + (float) (Math.cos(angle) * currentRadius);
                float circleY = y + (float) (Math.sin(angle) * currentRadius);

                buffer.vertex(matrix, circleX, circleY, 0)
                        .color(red, green, blue, alpha)
                        .endVertex();
            }

            tesselator.end();
        }

        pose.popPose();
    }

    private Vec3 transformToViewSpace(Vec3 relativePos, float partialTick, Player player) {
        double dx = relativePos.x;
        double dy = relativePos.y;
        double dz = relativePos.z;

        // Yaw rotation
        float yaw = player.getViewYRot(partialTick);
        double yawRad = Math.toRadians(-yaw);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        double x1 = dx * cosYaw - dz * sinYaw;
        double z1 = dx * sinYaw + dz * cosYaw;
        double y1 = dy;

        // Pitch rotation
        float pitch = player.getViewXRot(partialTick);
        double pitchRad = Math.toRadians(-pitch);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        double y2 = y1 * cosPitch - z1 * sinPitch;
        double z2 = y1 * sinPitch + z1 * cosPitch;
        double x2 = x1;

        if (z2 < 0) return null;

        return new Vec3(x2, y2, z2);
    }

    private double lerp(float partialTick, double old, double current) {
        return old + (current - old) * partialTick;
    }

    private boolean shouldRenderEntity(Entity entity) {
        if (Config.showAllCollidable) {
            AABB boundingBox = entity.getBoundingBox();
            if (boundingBox != null && !boundingBox.equals(AABB.ofSize(Vec3.ZERO, 0, 0, 0))) {
                return true;
            }
        }

        if (entity instanceof Enemy && Config.showEnemies) return true;
        if (entity instanceof Player && Config.showPlayers) return true;
        if (entity instanceof Projectile && Config.showProjectiles) return true;
        if (entity instanceof LivingEntity && Config.showOtherLiving) {
            if (!(entity instanceof Enemy) && !(entity instanceof Player)) {
                return true;
            }
        }

        return false;
    }

    private int getEntityColor(Entity entity) {
        if (entity instanceof Enemy) {
            return 0xFFFF0000;
        } else if (entity instanceof Player) {
            return 0xFF00FF00;
        } else if (entity instanceof Projectile) {
            return 0xFFFF00FF;
        } else if (entity instanceof LivingEntity) {
            return 0xFFFFFF00;
        } else {
            return 0xFF00FFFF;
        }
    }
}
