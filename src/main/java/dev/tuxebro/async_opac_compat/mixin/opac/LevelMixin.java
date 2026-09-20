package dev.tuxebro.async_opac_compat.mixin.opac;

import dev.tuxebro.async_opac_compat.ClaimsAreaChecker;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Level.class, priority = 100_000)
public abstract class LevelMixin {
    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;ZLnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/level/Explosion;", at = @At("HEAD"), cancellable = true)
    public void fixAsyncFromNoWorkyWithOPACForSomeReason(
            Entity entity,
            DamageSource damageSource,
            ExplosionDamageCalculator damageCalculator,
            double x, double y, double z,
            float radius, boolean fire,

            Level.ExplosionInteraction explosionInteraction,
            boolean spawnParticles,
            ParticleOptions smallExplosionParticles,
            ParticleOptions largeExplosionParticles,
            Holder<SoundEvent> explosionSound,
            CallbackInfoReturnable<Explosion> cir
    ) {
        if (entity == null) return;

        Level level = (Level) (Object) this;

        boolean isEntityNearClaim
                = ClaimsAreaChecker.checkIfEntityIsIn(level, entity);

        if (!isEntityNearClaim) return;

        Explosion explosion = new Explosion(
                level, entity, damageSource, damageCalculator,
                x, y, z, radius, false,
                Explosion.BlockInteraction.KEEP,
                smallExplosionParticles, largeExplosionParticles,
                explosionSound);
        explosion.explode();
        explosion.finalizeExplosion(spawnParticles);

        cir.setReturnValue(explosion);
    }
}