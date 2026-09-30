package net.jhbach.bastionfall.network;

import net.jhbach.bastionfall.client.BastionMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenBastionScreenPacket {
    private final BlockPos bastionPos;
    private final int minChunkX;
    private final int minChunkZ;
    private final int sizeX;
    private final int sizeZ;
    private final boolean[] claimed;

    public OpenBastionScreenPacket(BlockPos bastionPos, int minChunkX, int minChunkZ, int sizeX, int sizeZ, boolean[] claimed) {
        this.bastionPos = bastionPos;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.claimed = claimed;
    }

    public static void encode(OpenBastionScreenPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.bastionPos);
        buf.writeInt(pkt.minChunkX);
        buf.writeInt(pkt.minChunkZ);
        buf.writeInt(pkt.sizeX);
        buf.writeInt(pkt.sizeZ);
        buf.writeVarInt(pkt.claimed.length);
        for (boolean b : pkt.claimed) {
            buf.writeBoolean(b);
        }
    }

    public static OpenBastionScreenPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int minX = buf.readInt();
        int minZ = buf.readInt();
        int sizeX = buf.readInt();
        int sizeZ = buf.readInt();
        int len = buf.readVarInt();
        boolean[] claimed = new boolean[len];
        for (int i = 0; i < len; i++) {
            claimed[i] = buf.readBoolean();
        }
        return new OpenBastionScreenPacket(pos, minX, minZ, sizeX, sizeZ, claimed);
    }

    public static void handle(OpenBastionScreenPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.setScreen(new BastionMapScreen(pkt.bastionPos, pkt.minChunkX, pkt.minChunkZ, pkt.sizeX, pkt.sizeZ, pkt.claimed));
        });
        ctx.get().setPacketHandled(true);
    }
}
