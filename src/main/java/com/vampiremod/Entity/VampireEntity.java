package com.vampiremod.entity;

import com.vampiremod.sound.ModSounds;
import com.vampiremod.network.ModNetworking;
import com.vampiremod.network.OfferVampireBitePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public class VampireEntity extends AbstractIllager {

    private boolean offeringBite = false;
    private MeleeAttackGoal attackGoal;
    private WaterAvoidingRandomStrollGoal wanderGoal;
    private LookAtPlayerGoal lookGoal;
    private RandomLookAroundGoal lookAroundGoal;
    private HurtByTargetGoal hurtGoal;
    private NearestAttackableTargetGoal<Player> targetPlayerGoal;

    public VampireEntity(EntityType<? extends AbstractIllager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        attackGoal = new MeleeAttackGoal(this, 1.0D, true);
        wanderGoal = new WaterAvoidingRandomStrollGoal(this, 1.0D);
        lookGoal = new LookAtPlayerGoal(this, Player.class, 8.0F);
        lookAroundGoal = new RandomLookAroundGoal(this);
        hurtGoal = new HurtByTargetGoal(this);
        targetPlayerGoal = new NearestAttackableTargetGoal<>(this, Player.class, true);

        this.goalSelector.addGoal(1, attackGoal);
        this.goalSelector.addGoal(2, wanderGoal);
        this.goalSelector.addGoal(3, lookGoal);
        this.goalSelector.addGoal(4, lookAroundGoal);

        this.targetSelector.addGoal(1, hurtGoal);
        this.targetSelector.addGoal(2, targetPlayerGoal);
    }

    @Override
    public void applyRaidBuffs(int p_37844_, boolean p_37845_) {

    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!canDieFrom(source)) {
            float current = this.getHealth();
            if (current - amount <= 1.0F) {
                amount = Math.max(0.0F, current - 1.0F);
            }
            if (amount <= 0.0F) {
                return false;
            }
        }

        boolean hit = super.hurt(source, amount);

        if (!level().isClientSide && this.getHealth() <= 2.0F && !offeringBite) {
            enterOfferingState();
        }
        return hit;
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return ModSounds.VAMPIRE_DEATH.get();
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.VAMPIRE_AMBIENT.get();
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.VAMPIRE_HURT.get();
    }

    @Override
    public SoundEvent getDeathSound() {
        return ModSounds.VAMPIRE_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.VAMPIRE_STEP.get(), 0.15F, 1.0F);
    }

    private void enterOfferingState() {
        this.offeringBite = true;
        this.setTarget(null);
        this.setAggressive(false);
        this.getNavigation().stop();
        this.goalSelector.removeGoal(attackGoal);
        this.goalSelector.removeGoal(wanderGoal);
        this.targetSelector.removeGoal(hurtGoal);
        this.targetSelector.removeGoal(targetPlayerGoal);

        this.level().players().forEach(p -> {
            if (p.distanceTo(this) < 10.0F) {
                if (p instanceof ServerPlayer serverPlayer) {
                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new OfferVampireBitePacket(this.getId()));
                }
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        burnInSunlight();
    }

    private void burnInSunlight() {
        if (this.level().isClientSide) return;
        if (!this.level().isDay()) return;

        float brightness = this.getLightLevelDependentMagicValue();
        if (brightness > 0.5F) {
            var pos = this.blockPosition();
            if (this.level().canSeeSky(pos)) {
                this.setSecondsOnFire(8);
            }
        }
    }

    public void disperseIntoBats() {
        if (this.level().isClientSide || this.isRemoved()) return;

        for (int i = 0; i < 3; i++) {
            Bat bat = EntityType.BAT.create(this.level());
            if (bat != null) {
                double dx = this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D;
                double dy = this.getY() + 0.5D + this.random.nextDouble() * 0.5D;
                double dz = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D;
                bat.moveTo(dx, dy, dz, this.random.nextFloat() * 360.0F, 0.0F);
                bat.setDeltaMovement(this.random.triangle(0.0D, 0.2D), this.random.triangle(0.2D, 0.2D), this.random.triangle(0.0D, 0.2D));
                this.level().addFreshEntity(bat);
            }
        }
        this.discard();
    }

    private boolean canDieFrom(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }

        var attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            ItemStack held = living.getMainHandItem();
            return held.is(Items.WOODEN_SWORD);
        }
        return false;
    }

    public boolean isOfferingBite() {
        return offeringBite;
    }

    // AbstractIllager requires this:
    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        // Simple for now; can be customized later.
        return this.isAggressive() && !offeringBite
                ? AbstractIllager.IllagerArmPose.ATTACKING
                : AbstractIllager.IllagerArmPose.CROSSED;
    }
}
