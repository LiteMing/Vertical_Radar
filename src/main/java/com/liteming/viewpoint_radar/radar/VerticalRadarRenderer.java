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
    private static final int PLAYER_ICON_SIZE = 8;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        if (!Config.isRadarActive()) return;

        int radarWidth = (int) (screenWidth * Config.radarWidthScale);
        int radarHeight = (int) (screenHeight * Config.radarHeightScale);
        int radarCenterX = (int) (screenWidth * Config.radarPositionX);
        int radarCenterY = (int) (screenHeight * Config.radarPositionY - radarHeight / 2.0);

        Vec3 playerPos = player.getPosition(partialTick);

        double scanDistance = Config.entityScanDistance;
        AABB scanBox = new AABB(
                playerPos.x - scanDistance, playerPos.y - scanDistance, playerPos.z - scanDistance,
                playerPos.x + scanDistance, playerPos.y + scanDistance, playerPos.z + scanDistance
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        renderRadarBackground(guiGraphics, radarCenterX, radarCenterY, radarWidth, radarHeight);

        for (Entity entity : mc.level.getEntities(null, scanBox)) {
            if (entity == player) continue;
            if (!shouldRenderEntity(entity)) continue;

            renderEntityOnRadar(entity, playerPos, partialTick, radarCenterX, radarCenterY,
                    guiGraphics, radarWidth, radarHeight);
        }

        if (YOUKAI_LOADED && Config.showProjectiles) {
            renderYHDanmaku(playerPos, partialTick, radarCenterX, radarCenterY,
                    guiGraphics, mc);
        }

        renderPlayerIcon(guiGraphics, radarCenterX, radarCenterY);

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderYHDanmaku(Vec3 playerPos, float partialTick, int centerX, int centerY,
                                 GuiGraphics guiGraphics, Minecraft mc) {
        try {
            ClientDanmakuCache cache = ClientDanmakuCache.get(mc.level);

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

                float distToPlayer = (float) Math.sqrt(
                        Math.pow(renderX - centerX, 2) + Math.pow(renderY - centerY, 2)
                );

                float warningDistance = PLAYER_ICON_SIZE * 3.0f;
                float dangerDistance = PLAYER_ICON_SIZE + Config.dotSize / 20.0f;

                int color;
                int alpha;

                if (distToPlayer <= dangerDistance) {
                    color = 0xFFFF0000;
                    alpha = 255;
                } else if (distToPlayer <= warningDistance) {
                    float ratio = (warningDistance - distToPlayer) / (warningDistance - dangerDistance);
                    int red = 255;
                    int green = (int) (255 - ratio * 55);
                    int blue = (int) (200 - ratio * 200);
                    color = (red << 16) | (green << 8) | blue;
                    alpha = 255;
                } else {
                    color = 0xFFFF00FF;
                    float alphaFactor = 1.0f - (float)(viewDist / Config.maxDisplayDistance);
                    if (alphaFactor < 0.15f) alphaFactor = 0.15f;
                    alpha = (int) (alphaFactor * 255);
                }

                color = (color & 0x00FFFFFF) | (alpha << 24);

// 计算弹幕飞行进度：从扫描边界到玩家视角平面（z=0）
// transformedPos.z 表示弹幕在视角空间中的深度
// z 接近 maxDisplayDistance 时进度接近0（刚进入扫描范围）
// z 接近 0 时进度接近1（到达玩家视角平面）
                float progressRadius = calculateProgressRadius(transformedPos.z, Config.dotSize / 2.0f);

// 先绘制内部半透明进度圆
                if (Config.showProjectileProgress) {
                    renderProgressCircle(guiGraphics, renderX, renderY, progressRadius, color);
                }

// 再绘制外部空心圆
                renderCircle(guiGraphics, renderX, renderY, Config.dotSize / 2.0f, color);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**

     计算弹幕飞行进度的半径

     @param zDepth 弹幕在视角空间的z深度

     @param maxRadius 最大半径（空心圆的半径）

     @return 进度圆的半径
     */
    private float calculateProgressRadius(double zDepth, float maxRadius) {
// zDepth 范围：0 到 maxDisplayDistance
// 当 zDepth 接近 maxDisplayDistance 时，进度接近0
// 当 zDepth 接近 0 时，进度接近1

        double normalizedDepth = zDepth / Config.maxDisplayDistance;

// 反转：距离越近，进度越高
        double progress = 1 - normalizedDepth;

// 确保进度在 0-1 范围内
        progress = Math.max(0.0, Math.min(1, progress));


// 计算半径：从几乎不可见到填满整个圆
        float minRadius = (float) (maxRadius * Config.progressMinRadiusPercent); // 改这里
        return (float) (minRadius + (maxRadius - minRadius) * progress);
    }
    private void renderProgressCircle(GuiGraphics guiGraphics, float x, float y, float radius, int color) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = pose.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

// 提取原色并设置半透明
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = (int) (((color >> 24) & 0xFF) * Config.progressCircleAlpha);


        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y, 0).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / CIRCLE_SEGMENTS;
            float circleX = x + (float) (Math.cos(angle) * radius);
            float circleY = y + (float) (Math.sin(angle) * radius);
            buffer.vertex(matrix, circleX, circleY, 0).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();

        pose.popPose();
    }


    private void renderRadarBackground(GuiGraphics guiGraphics, int centerX, int centerY,
                                       int width, int height) {
        float radius = Math.min(width, height) / 2.0f * 0.9f;

        int alpha = (int) (Config.radarBackgroundAlpha * 255);
        int color = (alpha << 24);

        renderFilledCircle(guiGraphics, centerX, centerY, radius, color);
    }

    private void renderPlayerIcon(GuiGraphics guiGraphics, int centerX, int centerY) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = pose.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        int alpha = 255;
        int red = 255;
        int green = 255;
        int blue = 255;

        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float top = centerY - PLAYER_ICON_SIZE;
        float bottom = centerY + PLAYER_ICON_SIZE;
        float left = centerX - PLAYER_ICON_SIZE;
        float right = centerX + PLAYER_ICON_SIZE;

        buffer.vertex(matrix, centerX, top, 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, right, centerY, 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, centerX, centerY, 0).color(red, green, blue, alpha).endVertex();

        buffer.vertex(matrix, right, centerY, 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, centerX, bottom, 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, centerX, centerY, 0).color(red, green, blue, alpha).endVertex();

        buffer.vertex(matrix, centerX, bottom, 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, left, centerY, 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, centerX, centerY, 0).color(red, green, blue, alpha).endVertex();

        buffer.vertex(matrix, left, centerY, 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, centerX, top, 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, centerX, centerY, 0).color(red, green, blue, alpha).endVertex();

        tesselator.end();
        pose.popPose();
    }

    private void renderFilledCircle(GuiGraphics guiGraphics, float x, float y, float radius, int color) {
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

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y, 0).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / CIRCLE_SEGMENTS;
            float circleX = x + (float) (Math.cos(angle) * radius);
            float circleY = y + (float) (Math.sin(angle) * radius);
            buffer.vertex(matrix, circleX, circleY, 0).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();

        pose.popPose();
    }

    private void renderEntityOnRadar(Entity entity, Vec3 playerPos, float partialTick,
                                     int centerX, int centerY, GuiGraphics guiGraphics,
                                     int radarWidth, int radarHeight) {
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

        float maxRadius = Math.min(radarWidth, radarHeight) / 2.0f * 0.9f;
        float distFromCenter = (float) Math.sqrt(
                Math.pow(renderX - centerX, 2) + Math.pow(renderY - centerY, 2)
        );
        if (distFromCenter > maxRadius) return;

        int color = getEntityColor(entity);

        float alphaFactor = 1.0f - (float)(dist / Config.maxDisplayDistance);
        if (alphaFactor < 0.15f) alphaFactor = 0.15f;
        int alpha = (int) (alphaFactor * 255);
        color = (color & 0x00FFFFFF) | (alpha << 24);

        renderCircle(guiGraphics, renderX, renderY, Config.dotSize / 2.0f, color);
    }

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

        float innerRadius = Math.max(0, radius - (float)Config.outlineThickness);

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / CIRCLE_SEGMENTS;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            buffer.vertex(matrix, x + cos * innerRadius, y + sin * innerRadius, 0)
                    .color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, x + cos * radius, y + sin * radius, 0)
                    .color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();

        pose.popPose();
    }

    private Vec3 transformToViewSpace(Vec3 relativePos, float partialTick, Player player) {
        double dx = relativePos.x;
        double dy = relativePos.y;
        double dz = relativePos.z;

        float yaw = player.getViewYRot(partialTick);
        double yawRad = Math.toRadians(-yaw);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        double x1 = dx * cosYaw - dz * sinYaw;
        double z1 = dx * sinYaw + dz * cosYaw;

        float pitch = player.getViewXRot(partialTick);
        double pitchRad = Math.toRadians(-pitch);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        double y2 = dy * cosPitch - z1 * sinPitch;
        double z2 = dy * sinPitch + z1 * cosPitch;

        if (z2 < 0) return null;

        return new Vec3(x1, y2, z2);
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
            return !(entity instanceof Enemy) && !(entity instanceof Player);
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