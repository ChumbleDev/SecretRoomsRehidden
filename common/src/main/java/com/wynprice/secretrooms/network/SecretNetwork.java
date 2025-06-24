package com.wynprice.secretrooms.network;

import com.wynprice.secretrooms.platform.SecretRoomsServices;

public class SecretNetwork {
    public static final SecretNetwork INSTANCE = new SecretNetwork();

    private SecretNetwork() {}

    public void sendToServer(ApplyTexturePacket packet) {
        SecretRoomsServices.PLATFORM.sendPacketToServer(packet);
    }
} 