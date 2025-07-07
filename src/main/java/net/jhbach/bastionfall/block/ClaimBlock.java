package net.jhbach.bastionfall.block;

import net.jhbach.bastionfall.ClaimStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimBlock extends Block {

	public ClaimBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		System.out.println("BOOG THIS IS WORKING");

		ChunkPos chunkPos = new ChunkPos(pos);
		ClaimStorage claimStorage = ClaimStorage.get(serverLevel);

		if (placer instanceof Player player) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					ChunkPos currentChunk = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
					if (claimStorage.isChunkClaimed(currentChunk)) {
						UUID owner = claimStorage.getChunkOwner(currentChunk);
						if (!placer.getUUID().equals(owner)) {
							player.getInventory().add(player.getItemInHand(player.getUsedItemHand()));
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
							return;
						}
					} else {
						claimStorage.claimChunk(currentChunk, player.getUUID());

						if (isEdgeChunk(dx, dz)) {
							if (placer instanceof ServerPlayer) {
								showClaimBoxParticles((ServerLevel) level, chunkPos);
							}
						}
					}
				}
			}
		}
	}

	private final Map<ChunkPos, Boolean> activeDisplayChunks = new HashMap<>();

	@Override
	public @NotNull InteractionResult
	use(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
		@NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
			ChunkPos centerChunk = new ChunkPos(pos);

			// Toggle the active state for the chunk
			boolean isActive = activeDisplayChunks.getOrDefault(centerChunk, false);
			if (isActive) {
				activeDisplayChunks.put(centerChunk, false); // Stop particle spawning
				return InteractionResult.SUCCESS;
			}

			activeDisplayChunks.put(centerChunk, true); // Start particle spawning

			final int[] ticks = {0};
			final int duration = 20 * 60; // 60 seconds (20 ticks/sec)
			final ChunkPos centerChunkFinal = new ChunkPos(pos);

			MinecraftForge.EVENT_BUS.register(new Object() {
				@SubscribeEvent
				public void onTick(TickEvent.ServerTickEvent event) {
					if (event.phase != TickEvent.Phase.START) return;

					if (!activeDisplayChunks.getOrDefault(centerChunkFinal, false)) {
						MinecraftForge.EVENT_BUS.unregister(this); // Stop if toggled off
						return;
					}

					if (ticks[0] % 20 == 0) { // every second
						showClaimBoxParticles(serverLevel, centerChunkFinal);
					}

					ticks[0]++;
					if (ticks[0] >= duration) {
						MinecraftForge.EVENT_BUS.unregister(this);
					}
				}
			});

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.CONSUME; // or PASS if you want other interactions to work
	}

	private boolean isEdgeChunk(int dx, int dz) {
		return (dx == -1 || dx == 1 || dz == -1 || dz == 1);
	}

	private void showClaimBoxParticles(ServerLevel level, ChunkPos centerChunk) {
		int minChunkX = centerChunk.x - 1;
		int maxChunkX = centerChunk.x + 1;
		int minChunkZ = centerChunk.z - 1;
		int maxChunkZ = centerChunk.z + 1;

		int minY = level.getMinBuildHeight();
		int maxY = level.getMaxBuildHeight();

		// North and South edges (Z fixed, X varies)
		for (int x = minChunkX * 16; x <= maxChunkX * 16 + 15; x += 2) {
			spawnVerticalParticleColumn(level, x, minChunkZ * 16, minY, maxY);       // north edge
			spawnVerticalParticleColumn(level, x, maxChunkZ * 16 + 15, minY, maxY);  // south edge
		}

		// West and East edges (X fixed, Z varies)
		for (int z = minChunkZ * 16; z <= maxChunkZ * 16 + 15; z += 2) {
			spawnVerticalParticleColumn(level, minChunkX * 16, z, minY, maxY);       // west edge
			spawnVerticalParticleColumn(level, maxChunkX * 16 + 15, z, minY, maxY);  // east edge
		}

		spawnVerticalParticleColumn(level, minChunkX * 16, minChunkZ * 16, minY, maxY);       // NW
		spawnVerticalParticleColumn(level, maxChunkX * 16 + 15, minChunkZ * 16, minY, maxY);  // NE
		spawnVerticalParticleColumn(level, minChunkX * 16, maxChunkZ * 16 + 15, minY, maxY);  // SW
		spawnVerticalParticleColumn(level, maxChunkX * 16 + 15, maxChunkZ * 16 + 15, minY, maxY); // SE

	}




	private void spawnVerticalParticleColumn(ServerLevel level, int blockX, int blockZ, int minY, int maxY) {
		for (int y = minY; y <= maxY; y += 4) {
			level.sendParticles(
					ParticleTypes.GLOW,
					blockX + 0.5,
					y + 0.5,
					blockZ + 0.5,
					3,
					0, 0, 0,
					0
			);
		}
	}

	private void spawnVerticalLine(ServerLevel level, int x, int z, int minY, int maxY) {
		for (int y = minY; y <= maxY; y += 4) {
			level.sendParticles(
					ParticleTypes.GLOW,
					x + 0.5,
					y + 0.5,
					z + 0.5,
					1,
					0, 0, 0,
					0
			);
		}
	}




}
