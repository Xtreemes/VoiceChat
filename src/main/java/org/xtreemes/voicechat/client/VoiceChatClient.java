package org.xtreemes.voicechat.client;

import net.fabricmc.api.ClientModInitializer;
import org.xtreemes.voicechat.client.pipe.PipeManager;

public class VoiceChatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PipeManager.setClientID(910316364269494332L);
        PipeManager.initializePipe();

    }
}
