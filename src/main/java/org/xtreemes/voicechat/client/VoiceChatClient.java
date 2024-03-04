package org.xtreemes.voicechat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.jagrosh.discordipc.entities.pipe.Pipe;

public class VoiceChatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //PipeManager.setClientID(910316364269494332L);
        PipeManager.initializePipe();
        Pipe pipe;

        ClientTickEvents.START_CLIENT_TICK.register(clientt -> {
            System.out.println("test");
        });
    }
}
