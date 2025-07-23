package net.jhbach.bastionfall.gametest;

import com.mojang.authlib.GameProfile;
import net.jhbach.bastionfall.ClaimStorage;
import net.jhbach.bastionfall.block.ClaimBlock;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("bastionfall")
@PrefixGameTestTemplate(value = false)
public class ClaimBlockTest {

	private static ServerPlayer mockPlayer;

	@BeforeBatch(batch = "claim_block")
	public static void initTestReporter(ServerLevel level) {
		GameTestJUnitReporter.init();

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

	@GameTest(template = "claim_block", batch = "claim_block")
	public static void setPlacedBy_claimBlockPlaced_3x3Claim(GameTestHelper helper) {
		try {
			ServerLevel level = helper.getLevel();
			BlockPos blockPos = helper.absolutePos(new BlockPos(2, 0, 2));
			ChunkPos chunkPos = new ChunkPos(blockPos);

			ClaimStorage storage = ClaimStorage.get(level);
			storage.resetClaims();

			// Simulate placing the ClaimBlock
			ClaimBlock claimBlock = (ClaimBlock) ModBlocks.CLAIM_BLOCK.get();
			level.setBlock(blockPos, claimBlock.defaultBlockState(), 3);
			claimBlock.setPlacedBy(level, blockPos, claimBlock.defaultBlockState(), mockPlayer, ItemStack.EMPTY);

			// Verify chunks are claimed
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					ChunkPos currentChunk = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
					helper.assertTrue(storage.isChunkClaimed(currentChunk),
							"Expected " + dx + ", " + dz + " chunk to be claimed");
					helper.assertTrue(mockPlayer.getUUID().equals(storage.getChunkOwner(currentChunk)),
							"Chunk owner UUID mismatch");
				}
			}

			// Record test as passed
			GameTestJUnitReporter.recordPass(Thread.currentThread().getStackTrace()[1].getMethodName());
			helper.succeed();
		} catch (Throwable t) {
			// Record test as failed
			GameTestJUnitReporter.recordFail(Thread.currentThread().getStackTrace()[1].getMethodName(), t.getMessage());
		} finally {
			GameTestJUnitReporter.writeReport();
		}
	}

	@GameTest(template = "claim_block", batch = "claim_block")
	public void setPlacedBy_overlappingClaims_noOverride(GameTestHelper helper) {
		try {
			ServerLevel level = helper.getLevel();
			ClaimStorage claimStorage = ClaimStorage.get(level);
			claimStorage.resetClaims();

			// Create Player A
			UUID ownerA = UUID.fromString("00000000-0000-0000-0000-000000000002");
			ServerPlayer playerA = new ServerPlayer(
				level.getServer(),
				level,
				new GameProfile(ownerA, "PlayerA")
			);
			playerA.setUUID(ownerA);
			playerA.connection = new ServerGamePacketListenerImpl(
				level.getServer(),
				new Connection(null),
				playerA
			);
			level.addFreshEntity(playerA);

			// Create Player B
			UUID ownerB = UUID.fromString("00000000-0000-0000-0000-000000000003");
			ServerPlayer playerB = new ServerPlayer(
				level.getServer(),
				level,
				new GameProfile(ownerB, "PlayerB")
			);
			playerB.setUUID(ownerB);
			playerB.connection = new ServerGamePacketListenerImpl(
				level.getServer(),
				new Connection(null),
				playerB
			);
			level.addFreshEntity(playerB);

			// Place ClaimBlock for Player A
			BlockPos blockPosA = helper.absolutePos(new BlockPos(8, 1, 8));
			ClaimBlock claimBlock = (ClaimBlock) ModBlocks.CLAIM_BLOCK.get();
			level.setBlock(blockPosA, claimBlock.defaultBlockState(), 3);
			claimBlock.setPlacedBy(level, blockPosA, claimBlock.defaultBlockState(), playerA, ItemStack.EMPTY);

			// Place ClaimBlock for Player B overlapping A's claim
			BlockPos blockPosB = helper.absolutePos(new BlockPos(9, 1, 8));
			level.setBlock(blockPosB, claimBlock.defaultBlockState(), 3);
			claimBlock.setPlacedBy(level, blockPosB, claimBlock.defaultBlockState(), playerB, ItemStack.EMPTY);

			// Check that all chunks in Player A's claim area are still owned by Player A
			ChunkPos chunkPosA = new ChunkPos(blockPosA);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					ChunkPos currentChunk = new ChunkPos(chunkPosA.x + dx, chunkPosA.z + dz);
					UUID actualOwner = claimStorage.getChunkOwner(currentChunk);
					helper.assertTrue(actualOwner != null && actualOwner.equals(ownerA), "Chunk at " + currentChunk + " should remain owned by ownerA");
				}
			}

			GameTestJUnitReporter.recordPass(Thread.currentThread().getStackTrace()[1].getMethodName());
			helper.succeed();
		} catch (Throwable t) {
			GameTestJUnitReporter.recordFail(Thread.currentThread().getStackTrace()[1].getMethodName(), t.getMessage());
		} finally {
			GameTestJUnitReporter.writeReport();
		}
	}
}
