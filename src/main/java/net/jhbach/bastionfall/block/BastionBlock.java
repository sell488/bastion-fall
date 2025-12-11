package net.jhbach.bastionfall.block;

import net.jhbach.bastionfall.ClaimStorage;
import net.jhbach.bastionfall.network.BastionNetwork;
import net.jhbach.bastionfall.network.OpenBastionScreenPacket;
import net.minecraft.core.BlockPos;
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

public class BastionBlock extends Block {

        public BastionBlock(BlockBehaviour.Properties properties) {
                super(properties);
        }

        @Override
        @SuppressWarnings("resource")
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
                if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                        ClaimStorage.IslandSnapshot snapshot = ClaimStorage.get((ServerLevel) level)
                                        .buildIslandSnapshot(new ChunkPos(pos), 1);

                        BastionNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new OpenBastionScreenPacket(pos, snapshot.minX(), snapshot.minZ(),
                                                snapshot.sizeX(), snapshot.sizeZ(), snapshot.claimed()));
                        return InteractionResult.SUCCESS;
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
        }
}
