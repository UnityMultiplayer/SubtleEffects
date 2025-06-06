package einstein.subtle_effects.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import einstein.subtle_effects.init.ModParticles;
import einstein.subtle_effects.particle.SparkParticle;
import einstein.subtle_effects.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static einstein.subtle_effects.init.ModConfigs.BIOMES;
import static einstein.subtle_effects.init.ModConfigs.BLOCKS;
import static einstein.subtle_effects.util.MathUtil.nextSign;
import static net.minecraft.util.Mth.nextFloat;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements FrustumGetter {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    private Frustum cullingFrustum;

    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapOperation(method = "renderSnowAndRain", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;RAIN_LOCATION:Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation replaceRainTexture(Operation<ResourceLocation> original) {
        if (BIOMES.biomeColorRain) {
            return Util.COLORLESS_RAIN_TEXTURE;
        }
        return original.call();
    }

    @WrapOperation(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer renderSnowAndRain(VertexConsumer instance, float red, float green, float blue, float alpha, Operation<VertexConsumer> original, @Local Biome biome, @Local Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN && BIOMES.biomeColorRain) {
            int waterColor = biome.getWaterColor();
            return instance.setColor((waterColor >> 16) / 255F, (waterColor >> 8) / 255F, waterColor / 255F, alpha);
        }
        return original.call(instance, red, green, blue, alpha);
    }

    @Inject(method = "levelEvent", at = @At("TAIL"))
    private void levelEvent(int type, BlockPos pos, int data, CallbackInfo ci) {
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        BlockState state = level.getBlockState(pos);
        Player player = minecraft.player;

        switch (type) {
            case 1029: {
                if (BLOCKS.anvilBreakParticles) {
                    level.addDestroyBlockEffect(pos, state);
                }
                break;
            }
            case 1030: {
                if (BLOCKS.anvilUseParticles) {
                    float pointX = random.nextFloat();
                    float pointZ = random.nextFloat();

                    for (int i = 0; i < 20; i++) {
                        int xSign = nextSign(random);
                        int zSign = nextSign(random);
                        level.addParticle(SparkParticle.create(SparkType.METAL, random),
                                pos.getX() + pointX,
                                pos.getY() + 1,
                                pos.getZ() + pointZ,
                                nextFloat(random, 0.1F, 0.2F) * xSign,
                                nextFloat(random, 0.1F, 0.2F),
                                nextFloat(random, 0.1F, 0.2F) * zSign
                        );
                    }
                }
                break;
            }
            case 1042: {
                if (BLOCKS.grindstoneUseParticles) {
                    if (state.hasProperty(GrindstoneBlock.FACING) && state.hasProperty(GrindstoneBlock.FACE)) {
                        Direction direction = state.getValue(GrindstoneBlock.FACING);
                        AttachFace face = state.getValue(GrindstoneBlock.FACE);
                        Direction side = face == AttachFace.CEILING ? Direction.DOWN : Direction.UP;

                        for (int i = 0; i < 20; i++) {
                            ParticleSpawnUtil.spawnParticlesOnSide(SparkParticle.create(SparkType.METAL, random), 0, side, level, pos, random,
                                    nextFloat(random, 0.1F, 0.2F) * (direction.getStepX() * 1.5),
                                    face == AttachFace.CEILING ? 0 : nextFloat(random, 0.1F, 0.2F),
                                    nextFloat(random, 0.1F, 0.2F) * (direction.getStepZ() * 1.5)
                            );
                        }
                    }
                }
                break;
            }
        }
    }

    @WrapOperation(method = "levelEvent", at = @At(value = "FIELD", target = "Lnet/minecraft/core/particles/ParticleTypes;LARGE_SMOKE:Lnet/minecraft/core/particles/SimpleParticleType;"))
    private SimpleParticleType replaceSmoke(Operation<SimpleParticleType> original) {
        if (BLOCKS.steam.lavaFizzSteam) {
            return ModParticles.STEAM.get();
        }
        return original.call();
    }

    @WrapOperation(method = "levelEvent", at = @At(value = "FIELD", target = "Lnet/minecraft/core/particles/ParticleTypes;CLOUD:Lnet/minecraft/core/particles/SimpleParticleType;"))
    private SimpleParticleType replaceCloud(Operation<SimpleParticleType> original) {
        if (BLOCKS.steam.spongeDryingOutSteam) {
            return ModParticles.STEAM.get();
        }
        return original.call();
    }

    @ModifyReturnValue(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At(value = "RETURN", ordinal = 0))
    private Particle spawnForcedParticle(Particle particle) {
        if (particle != null) {
            ((ParticleAccessor) particle).subtleEffects$force();
        }
        return particle;
    }

    @Override
    public Frustum subtleEffects$getCullingFrustum() {
        return cullingFrustum;
    }
}
