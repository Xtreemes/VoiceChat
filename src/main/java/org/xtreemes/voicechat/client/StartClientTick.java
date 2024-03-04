package org.xtreemes.voicechat.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.json.JSONObject;
import org.xtreemes.voicechat.client.pipe.PipeManager;

import java.util.Set;

public class StartClientTick implements ClientTickEvents.StartTick {

    @Override
    public void onStartTick(MinecraftClient client) {
        String active_vc = VoiceChatManager.getActiveVC();
        boolean state = VoiceChatManager.getVoiceChatState();
        if(state) {
            JSONObject json = new JSONObject()
                    .put("cmd", "GET_CHANNEL")
                    .put("args", new JSONObject()
                            .put("channel_id", active_vc));
            PipeManager.sendThroughPipe(json);

            Set<String> user_map = VoiceChatManager.getUserNames();
            for(String user : user_map){
                int default_volume = VoiceChatManager.getDefaultVolume(user);

                float volume_ratio = (float) default_volume / 100;
                int true_volume = (int) (VoiceChatManager.getUser(user) * volume_ratio);

                if(true_volume < 100) {
                    true_volume = ((-1200 / (true_volume - 111)) - 11);
                }
                true_volume = true_volume < 0 ? 0 : Math.min(true_volume, 200);

                JSONObject volume_json = new JSONObject()
                        .put("cmd","SET_USER_VOICE_SETTINGS")
                        .put("args", new JSONObject()
                                .put("user_id",VoiceChatManager.getUserID(user))
                                .put("volume", true_volume));
                PipeManager.sendThroughPipe(volume_json);
            }
        }
    }
}
