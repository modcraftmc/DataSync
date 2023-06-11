package fr.modcraftmc.datasync.networking;

import fr.modcraftmc.datasync.networking.packets.IPacket;
import fr.modcraftmc.datasync.networking.packets.PacketUpdateClusterPlayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Network {
    private final SimpleChannel channel = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("datasync", "main_channel"))
            .clientAcceptedVersions("1"::equals)
            .serverAcceptedVersions("1"::equals)
            .networkProtocolVersion(() -> "1")
            .simpleChannel();

    public void Init() {
        channel.registerMessage(0, PacketUpdateClusterPlayers.class, IPacket::encode, PacketUpdateClusterPlayers::decode, IPacket::handle);
    }

    public <MSG> void sendToServer(MSG message) {
        channel.sendToServer(message);
    }

    public <MSG> void sendTo(MSG message, ServerPlayer player) {
        channel.sendTo(message, player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
    }

    public <MSG> void sendToAllPlayers(MSG message) {
        channel.send(PacketDistributor.ALL.noArg(), message);
    }
}
