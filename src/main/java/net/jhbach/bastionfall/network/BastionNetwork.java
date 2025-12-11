package net.jhbach.bastionfall.network;

import net.jhbach.bastionfall.BastionFall;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class BastionNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BastionFall.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, OpenBastionScreenPacket.class,
                OpenBastionScreenPacket::encode,
                OpenBastionScreenPacket::decode,
                OpenBastionScreenPacket::handle);
    }
}
