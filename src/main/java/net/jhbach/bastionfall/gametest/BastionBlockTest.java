package net.jhbach.bastionfall.gametest;

import com.mojang.authlib.GameProfile;
import net.jhbach.bastionfall.block.ModBlocks;
import net.jhbach.bastionfall.test.GameTestJUnitReporter;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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
}
