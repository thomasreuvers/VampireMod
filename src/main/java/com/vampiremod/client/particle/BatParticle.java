package com.vampiremod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class BatParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected BatParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.lifetime = 10 + this.random.nextInt(10);
        this.quadSize = 0.8F + this.random.nextFloat() * 0.3F;
        this.gravity = 0.0F;
        this.friction = 0.92F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        // Slight upward drift to keep bats visible
        this.yd += 0.003D;
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float ageFactor = Mth.clamp(((float) this.age + partialTicks) / (float) this.lifetime, 0.0F, 1.0F);
        return this.quadSize * (1.0F - ageFactor * 0.3F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new BatParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
