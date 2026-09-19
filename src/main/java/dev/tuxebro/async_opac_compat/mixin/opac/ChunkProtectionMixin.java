package dev.tuxebro.async_opac_compat.mixin.opac;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.pac.common.server.IServerData;
import xaero.pac.common.server.claims.protection.ChunkProtection;

import java.util.List;

@Mixin(ChunkProtection.class)
public class ChunkProtectionMixin {
    @Inject(method = "onExplosionDetonate", at = @At("HEAD"), cancellable = true)
    public void cancelDetonationHandlingBecauseItsUseless(IServerData<?, ?> serverData, ServerLevel world, Explosion explosion, List<Entity> affectedEntities, List<BlockPos> affectedBlocks, CallbackInfo ci) {
        ci.cancel();
    }
}
