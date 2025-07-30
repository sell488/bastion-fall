package net.jhbach.bastionfall;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimStorage extends SavedData {
	private static final String DATA_NAME = "bastionfall_claims";

	private final Map<ChunkPos, UUID> claims = new HashMap<>();

	public static ClaimStorage load(CompoundTag nbt) {
		ClaimStorage storage = new ClaimStorage();
		ListTag list = nbt.getList("Claims", ListTag.TAG_COMPOUND);
		for (Tag t : list) {
			CompoundTag entry = (CompoundTag) t;
			int cx = entry.getInt("ChunkX");
			int cz = entry.getInt("ChunkZ");
			UUID owner = entry.getUUID("Owner");
			storage.claims.put(new ChunkPos(cx, cz), owner);
		}
		return storage;
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		ListTag list = new ListTag();
		for (Map.Entry<ChunkPos, UUID> e: claims.entrySet()) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("ChunkX", e.getKey().x);
			entry.putInt("ChunkZ", e.getKey().z);
			entry.putUUID("Owner", e.getValue());
			list.add(entry);
		}
		nbt.put("Claims", list);
		return nbt;
	}

	public ClaimStorage() { }

	public static ClaimStorage get(ServerLevel level) {
		if(level.dimension() != level.getServer().overworld().dimension()) {
			throw new IllegalStateException("ClaimStorage only exists in the Overworld");
		}
		return level.getDataStorage()
				.computeIfAbsent(ClaimStorage::load, ClaimStorage::new, DATA_NAME);
	}

	public boolean isChunkClaimed(ChunkPos pos) {
		return claims.containsKey(pos);
	}

	public UUID getChunkOwner(ChunkPos chunk) {
		return claims.get(chunk);
	}

	public void claimChunk(ChunkPos pos, UUID owner) {
		if (!claims.containsKey(pos)) {
			claims.put(pos, owner);
			setDirty();
		}
	}

	public void unclaimChunk(ChunkPos pos) {
		if(claims.remove(pos) != null) {
			setDirty();
		}
	}

	public void claimChunksAround(ChunkPos pos, UUID owner, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				ChunkPos currentChunk = new ChunkPos(pos.x + dx, pos.z + dz);
				claimChunk(currentChunk, owner);
			}
		}
	}

	public void unclaimChunksAround(ChunkPos chunk, UUID owner, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				ChunkPos currentChunk = new ChunkPos(chunk.x + dx, chunk.z + dz);
				if(owner.equals(claims.get(currentChunk))) {
					unclaimChunk(currentChunk);
				}
			}
		}
	}

	public Map<ChunkPos, UUID> getClaimStorage() {
		return claims;
	}

	@VisibleForTesting
	public void resetClaims() {
		claims.clear();
	}
}
