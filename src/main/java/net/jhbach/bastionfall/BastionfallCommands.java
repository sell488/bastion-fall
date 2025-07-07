package net.jhbach.bastionfall;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class BastionfallCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("bastionfall")
						.then(Commands.literal("testclaim")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									ServerLevel level = (ServerLevel) player.level();

									if (!level.dimension().equals(Level.OVERWORLD)) {
										player.sendSystemMessage(Component.literal("Only works in Overworld"));
										return 1;
									}

									ChunkPos chunk = new ChunkPos(player.blockPosition());
									ClaimStorage storage = ClaimStorage.get(level);

									if (storage.isChunkClaimed(chunk)) {
										UUID owner = storage.getChunkOwner(chunk);
										MinecraftServer server = player.getServer();

										if (server != null) {
											GameProfile profile = server.getProfileCache().get(owner).orElse(null);
											if (profile != null) {
												player.sendSystemMessage(Component.literal("Chunk is claimed by " + profile.getName()));
											} else {
												player.sendSystemMessage(Component.literal("Chunk is claimed by unknown player (" + owner + ")"));
											}
										} else {
											player.sendSystemMessage(Component.literal("Chunk is claimed, but server info is unavailable (" + owner + ")"));
										}
									} else {
										storage.claimChunk(chunk, player.getUUID());
										player.sendSystemMessage(Component.literal("Chunk claimed successfully"));
									}

									return 1;
								}))
		);
		registerResetChunkCommand(dispatcher);
	}

	private static void registerResetChunkCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("bastionfall")
						.then(Commands.literal("resetChunks")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									ServerLevel level = (ServerLevel) player.level();

									if (!level.dimension().equals(Level.OVERWORLD)) {
										player.sendSystemMessage(Component.literal("Only works in Overworld"));
										return 1;
									}

									ClaimStorage storage = ClaimStorage.get(level);
									storage.resetClaims();
									player.sendSystemMessage(Component.literal("All claimed chunks have been reset"));

									return 1;
								}))
		);
	}
}
