package net.jhbach.bastionfall;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClaimStorage extends SavedData {
        private static final String DATA_NAME = "bastionfall_claims";

        private final Map<ChunkPos, UUID> claims = new HashMap<>();
        private final Map<IslandCacheKey, CachedIsland> islandCache = new HashMap<>();
        private long revision = 0;

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
                        markUpdated();
                }
        }

        public void unclaimChunk(ChunkPos pos) {
                if(claims.remove(pos) != null) {
                        markUpdated();
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
                markUpdated();
        }

        public IslandSnapshot buildIslandSnapshot(ChunkPos start, int padding) {
                long currentRevision = revision;
                IslandCacheKey key = new IslandCacheKey(start, padding);
                CachedIsland cached = islandCache.get(key);
                if (cached != null && cached.revision == currentRevision) {
                        return cached.snapshot;
                }

                Set<ChunkPos> visited = new HashSet<>();
                ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
                queue.add(start);
                visited.add(start);

                while (!queue.isEmpty()) {
                        ChunkPos current = queue.poll();
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                                ChunkPos neighbor = new ChunkPos(current.x + dir.getStepX(), current.z + dir.getStepZ());
                                if (!visited.contains(neighbor) && isChunkClaimed(neighbor)) {
                                        visited.add(neighbor);
                                        queue.add(neighbor);
                                }
                        }
                }

                int minX = visited.stream().mapToInt(c -> c.x).min().orElse(start.x) - padding;
                int maxX = visited.stream().mapToInt(c -> c.x).max().orElse(start.x) + padding;
                int minZ = visited.stream().mapToInt(c -> c.z).min().orElse(start.z) - padding;
                int maxZ = visited.stream().mapToInt(c -> c.z).max().orElse(start.z) + padding;

                int sizeX = maxX - minX + 1;
                int sizeZ = maxZ - minZ + 1;
                boolean[] claimed = new boolean[sizeX * sizeZ];
                for (int x = 0; x < sizeX; x++) {
                        for (int z = 0; z < sizeZ; z++) {
                                ChunkPos cp = new ChunkPos(minX + x, minZ + z);
                                if (isChunkClaimed(cp)) {
                                        claimed[x + z * sizeX] = true;
                                }
                        }
                }

                IslandSnapshot snapshot = new IslandSnapshot(minX, minZ, sizeX, sizeZ, claimed);
                islandCache.put(key, new CachedIsland(snapshot, currentRevision));
                return snapshot;
        }

        private void markUpdated() {
                revision++;
                islandCache.clear();
                setDirty();
        }

        public record IslandSnapshot(int minX, int minZ, int sizeX, int sizeZ, boolean[] claimed) { }

        private record IslandCacheKey(ChunkPos start, int padding) { }

        private record CachedIsland(IslandSnapshot snapshot, long revision) { }
}
