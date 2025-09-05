package net.jhbach.bastionfall.block;

import net.jhbach.bastionfall.ClaimStorage;
import net.jhbach.bastionfall.network.BastionNetwork;
import net.jhbach.bastionfall.network.OpenBastionScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class BastionBlock extends Block {

        public BastionBlock(BlockBehaviour.Properties properties) {
                super(properties);
        }

        @Override
        @SuppressWarnings("resource")
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
                if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                        ClaimStorage storage = ClaimStorage.get((ServerLevel) level);
                        ChunkPos start = new ChunkPos(pos);
                        Set<ChunkPos> island = new HashSet<>();
                        Queue<ChunkPos> queue = new ArrayDeque<>();
                        queue.add(start);
                        island.add(start);
                        while (!queue.isEmpty()) {
                                ChunkPos current = queue.poll();
                                for (Direction dir : Direction.Plane.HORIZONTAL) {
                                        ChunkPos neighbor = new ChunkPos(current.x + dir.getStepX(), current.z + dir.getStepZ());
                                        if (!island.contains(neighbor) && storage.isChunkClaimed(neighbor)) {
                                                island.add(neighbor);
                                                queue.add(neighbor);
                                        }
                                }
                        }

                        int minX = island.stream().mapToInt(c -> c.x).min().orElse(start.x);
                        int maxX = island.stream().mapToInt(c -> c.x).max().orElse(start.x);
                        int minZ = island.stream().mapToInt(c -> c.z).min().orElse(start.z);
                        int maxZ = island.stream().mapToInt(c -> c.z).max().orElse(start.z);

                        minX -= 1;
                        maxX += 1;
                        minZ -= 1;
                        maxZ += 1;

                        int sizeX = maxX - minX + 1;
                        int sizeZ = maxZ - minZ + 1;
                        boolean[] claimed = new boolean[sizeX * sizeZ];
                        for (int x = 0; x < sizeX; x++) {
                                for (int z = 0; z < sizeZ; z++) {
                                        ChunkPos cp = new ChunkPos(minX + x, minZ + z);
                                        if (storage.isChunkClaimed(cp)) {
                                                claimed[x + z * sizeX] = true;
                                        }
                                }
                        }

                        BastionNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new OpenBastionScreenPacket(pos, minX, minZ, sizeX, sizeZ, claimed));
                        return InteractionResult.SUCCESS;
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
        }
}
