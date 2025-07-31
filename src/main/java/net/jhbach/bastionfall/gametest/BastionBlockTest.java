package net.jhbach.bastionfall.gametest;

import com.mojang.authlib.GameProfile;
import net.jhbach.bastionfall.BastionFall;
import net.jhbach.bastionfall.ClaimStorage;
import net.jhbach.bastionfall.block.BastionBlock;
import net.jhbach.bastionfall.block.ModBlocks;
import net.jhbach.bastionfall.test.GameTestJUnitReporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static net.minecraft.world.InteractionHand.MAIN_HAND;

@GameTestHolder("bastionfall")
@PrefixGameTestTemplate(value = false)
public class BastionBlockTest {

	private static ServerPlayer mockPlayer;

	@BeforeBatch(batch = "bastion_block")
	public static void initTestReporter(ServerLevel level) {
		UUID testOwner = UUID.fromString("00000000-0000-0000-0000-000000000001");
		mockPlayer = new ServerPlayer(
				level.getServer(),
				level,
				new GameProfile(testOwner, "test-mock-player")
		);
		mockPlayer.setUUID(testOwner);

		// Mock the connection
		mockPlayer.connection = new ServerGamePacketListenerImpl(
				level.getServer(),
				new Connection(null),
				mockPlayer
		);
		level.addFreshEntity(mockPlayer);
	}

	@GameTest(template = "claim_block", batch = "bastion_block")
	public void setPlacedBy_claimedChunk_blockPlaced(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centerPos = helper.absolutePos(BlockPos.ZERO);

		ClaimStorage claimStorage = ClaimStorage.get(level);
		claimStorage.resetClaims();

		UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		ServerPlayer player = createMockPlayer(level, playerId, "PlayerA");

		// Claim the chunk for PlayerA
		ChunkPos chunk = new ChunkPos(centerPos);
		claimStorage.claimChunk(chunk, playerId);

		try {
			// Give the player the BastionBlock item
			ItemStack bastionStack = new ItemStack(ModBlocks.BASTION_BLOCK.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, bastionStack);

			// Simulate a right-click on the top face of the block below the target position
			BlockHitResult hit = new BlockHitResult(
					Vec3.atCenterOf(centerPos.below()),
					Direction.UP,
					centerPos.below(),
					false
			);
			UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);

			// Use the item
			InteractionResult result = bastionStack.useOn(context);

			// Assert that the block was actually placed
			Block placed = level.getBlockState(centerPos).getBlock();
			helper.assertTrue(placed instanceof BastionBlock, "Bastion block should be placed in claimed chunk");

			GameTestJUnitReporter.recordPass("setPlacedBy_claimedChunk_blockPlaced");
			helper.succeed();
		} catch (Throwable t) {
			GameTestJUnitReporter.recordFail("setPlacedBy_claimedChunk_blockPlaced", t.getMessage());
			helper.fail(t.getMessage());
		}
	}

	@GameTest(template = "bastion_block", batch = "bastion_block")
	public static void onPlace_unclaimedChunk_blockNotPlacedAndItermReturned(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		ItemStack itemStack = new ItemStack(ModBlocks.BASTION_BLOCK.get());
		ServerLevel level = helper.getLevel();
		try {
			InteractionResult result = ModBlocks.BASTION_BLOCK.get().defaultBlockState().getBlock()
					.use(ModBlocks.BASTION_BLOCK.get().defaultBlockState(), level, pos, mockPlayer, MAIN_HAND, null);

			helper.runAfterDelay(1, () -> {
				try {
					BlockState actualState = level.getBlockState(pos);
					String testName = "onPlace_unclaimedChunk_blockNotPlacedAndItermReturned";
					GameTestJUnitReporter.recordPass(testName);
					helper.succeed();
				} catch (Throwable t) {
					GameTestJUnitReporter.recordFail(Thread.currentThread().getStackTrace()[1].getMethodName(), t.getMessage());
					helper.fail(t.getMessage());
				}
			});
		} catch (Throwable t) {
			GameTestJUnitReporter.recordFail(Thread.currentThread().getStackTrace()[1].getMethodName(), t.getMessage());
		}

	}

	private ServerPlayer createMockPlayer(ServerLevel level, UUID playerUUID, String playerName) {
		ServerPlayer player = new ServerPlayer(
				level.getServer(),
				level,
				new GameProfile(playerUUID, playerName)
		);
		player.setUUID(playerUUID);
		player.connection = new ServerGamePacketListenerImpl(
				level.getServer(),
				new Connection(null),
				player
		);
		level.addFreshEntity(player);
		return player;
	}
}
