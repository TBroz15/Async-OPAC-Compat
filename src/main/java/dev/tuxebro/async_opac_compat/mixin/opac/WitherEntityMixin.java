package dev.tuxebro.async_opac_compat.mixin.opac;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.tuxebro.async_opac_compat.ClaimsAreaChecker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WitherBoss.class,priority = 100_000)
public class WitherEntityMixin {
    @WrapOperation(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;canEntityGrief(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean preventWitherFromWreckingShi(Level level, Entity entity, Operation<Boolean> original) {
        var originalValue = original.call(level, entity);

        boolean isEntityNearClaim
                = ClaimsAreaChecker.checkIfEntityNearClaim(level, entity);
        if (!isEntityNearClaim) return originalValue;

        return false;
    }
}
