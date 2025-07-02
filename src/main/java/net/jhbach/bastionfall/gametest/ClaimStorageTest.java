package net.jhbach.bastionfall.gametest;

import net.jhbach.bastionfall.ClaimStorage;
import net.jhbach.bastionfall.test.GameTestJUnitReporter;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("bastionfall")
@PrefixGameTestTemplate(value = false)
public class ClaimStorageTest {

	@BeforeBatch(batch="claimBlock")
	public static void initTestReporter(ServerLevel level) {
		GameTestJUnitReporter.init();
	}

	@GameTest(template = "claimblock", batch = "claimBlock")
	public static void testClaimStorageWorks2(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		ChunkPos pos = new ChunkPos(helper.absolutePos(new BlockPos(2, 0, 2))); // get chunk pos for world position
		UUID testOwner = UUID.fromString("00000000-0000-0000-0000-000000000001"); // deterministic for test

		ClaimStorage storage = ClaimStorage.get(level);
		storage.resetClaims();

		if (storage.isChunkClaimed(pos)) {
			GameTestJUnitReporter.recordFail("testClaimStorageWorks2", "Expected chunk to be unclaimed");
			GameTestJUnitReporter.writeReport();
			helper.fail("Expected chunk to be unclaimed");
		}

		GameTestJUnitReporter.recordPass("testClaimStorageWorks2");
		GameTestJUnitReporter.writeReport();
	}

	@GameTest(template = "claimblock", batch = "claimBlock")
	public static void testClaimStorageWorks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		ChunkPos pos = new ChunkPos(helper.absolutePos(new BlockPos(2, 0, 2))); // get chunk pos for world position
		UUID testOwner = UUID.fromString("00000000-0000-0000-0000-000000000001"); // deterministic for test

		ClaimStorage storage = ClaimStorage.get(level);
		storage.resetClaims();

		if (storage.isChunkClaimed(pos)) {
			GameTestJUnitReporter.recordFail("testClaimStorageWorks", "Expected chunk to be unclaimed");
			GameTestJUnitReporter.writeReport();
			helper.fail("Expected chunk to be unclaimed");
		}

		storage.claimChunk(pos, testOwner);

		if (!storage.isChunkClaimed(pos)) {
			GameTestJUnitReporter.recordFail("testClaimStorageWorks", "Expected chunk to be claimed");
			GameTestJUnitReporter.writeReport();
			helper.fail("Expected chunk to be claimed");
		}

		if (!testOwner.equals(storage.getChunkOwner(pos))) {
			GameTestJUnitReporter.recordFail("testClaimStorageWorks", "Chunk owner UUID mismatch");
			GameTestJUnitReporter.writeReport();
			helper.fail("Chunk owner UUID mismatch");
		}
		GameTestJUnitReporter.recordPass("testClaimStorageWorks");
		GameTestJUnitReporter.writeReport();
		helper.succeed();
	}


}
