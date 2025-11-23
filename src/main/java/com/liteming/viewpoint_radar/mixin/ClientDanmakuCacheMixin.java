package com.liteming.viewpoint_radar.mixin;

import com.liteming.viewpoint_radar.accessor.ClientDanmakuCacheAccessor;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.render.virtual.ClientDanmakuCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.LinkedList;

@Mixin(value = ClientDanmakuCache.class, remap = false)
public abstract class ClientDanmakuCacheMixin implements ClientDanmakuCacheAccessor {

    @Shadow
    private LinkedList<SimplifiedProjectile> all;

    @Override
    public Iterable<SimplifiedProjectile> viewpointRadar$getAllProjectiles() {
        return all;
    }
}
