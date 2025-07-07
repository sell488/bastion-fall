package net.jhbach.bastionfall.gametest;

import net.jhbach.bastionfall.ClaimStorage;
import net.jhbach.bastionfall.block.ClaimBlock;
import net.jhbach.bastionfall.block.ModBlocks;
import net.jhbach.bastionfall.test.GameTestJUnitReporter;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("bastionfall")
@PrefixGameTestTemplate(value = false)
public class ClaimBlockTest {

	@BeforeBatch(batch = "claimBlock")
	public static void initTestReporter(ServerLevel level) {
		GameTestJUnitReporter.init();
	}

	@GameTest(template = "claimBlock", batch = "claimBlock")
	public static void testClaimBlockPlacement(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos blockPos = helper.absolutePos(new BlockPos(2, 0, 2));
		ChunkPos chunkPos = new ChunkPos(blockPos);
		UUID testOwner = UUID.fromString("00000000-0000-0000-0000-000000000001");

		ClaimStorage storage = ClaimStorage.get(level);
		storage.resetClaims();

		// Simulate placing the ClaimBlock
		ClaimBlock claimBlock = (ClaimBlock) ModBlocks.CLAIM_BLOCK.get(); // Replace with actual ClaimBlock instance
		level.setBlock(blockPos, claimBlock.defaultBlockState(), 3);

		// Verify chunks are claimed
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				ChunkPos currentChunk = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
				if (!storage.isChunkClaimed(currentChunk)) {
					GameTestJUnitReporter.recordFail(Thread.currentThread().getStackTrace()[1].getMethodName(), "Expected chunk to be claimed");
					GameTestJUnitReporter.writeReport();
					helper.fail("Expected chunk to be claimed");
				}
				if (!testOwner.equals(storage.getChunkOwner(currentChunk))) {
					GameTestJUnitReporter.recordFail(Thread.currentThread().getStackTrace()[1].getMethodName(), "Chunk owner UUID mismatch");
					GameTestJUnitReporter.writeReport();
					helper.fail("Chunk owner UUID mismatch");
				}
			}
		}

		GameTestJUnitReporter.recordPass(Thread.currentThread().getStackTrace()[1].getMethodName());
		GameTestJUnitReporter.writeReport();
		helper.succeed();
	}
}