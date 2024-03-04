package org.xtreemes.voicechat.client;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xtreemes.voicechat.client.pipe.PipeManager;

import java.util.HashMap;
import java.util.Set;

public class VoiceChatManager {
    private static String active_vc = null;
    private static final HashMap<String, Integer> users = new HashMap<>();
    private static final HashMap<String, String> userIDs = new HashMap<>();

    // nickname, default volume
    private static final HashMap<String, Integer> default_volumes = new HashMap<>();
    private static boolean is_in_voicechat = false;

    public static String getActiveVC() {
        return active_vc;
    }

    public static void setVoiceChatState(boolean state){
        if(!state){
            users.clear();
            default_volumes.clear();
            userIDs.clear();
        }
        is_in_voicechat = state;
    }
    public static boolean getVoiceChatState(){
        return is_in_voicechat;
    }

    public static void setActiveVC(String vc) {
        active_vc = vc;
        if(vc != null) {
            JSONObject payload = new JSONObject()
                    .put("args", new JSONObject()
                            .put("channel_id", vc))
                    .put("cmd", "GET_CHANNEL");
            PipeManager.sendThroughPipe(payload);
        } else {
            setVoiceChatState(false);
        }
    }

    public static void setUser(String name, String volume){
        int v;
        try {
            v = Integer.parseInt(volume);
        } catch (NumberFormatException e) {
            System.out.println("Incorrrect volume! Not an integer?");
            v = 0;
        }
        users.put(name, v);
    }

    public static int getUser(String name){
        return users.getOrDefault(name, -100);
    }
    public static Set<String> getUserNames() {
        return users.keySet();
    }

    public static void setDefaultVolumes(JSONArray array){
        for(int i = 0; i < array.length(); i++){
            JSONObject object = array.getJSONObject(i);
            int volume = object.getInt("volume");
            volume = volume < 100 ? (-1200/(volume+11))+111: volume;

            default_volumes.put(object.getString("nick"),volume);
        }
    }
    public static int getDefaultVolume(String name){
        return default_volumes.getOrDefault(name, 100);
    }


    public static void setUserID(String name, String id){
        userIDs.put(name, id);
    }
    public static String getUserID(String name){
        return userIDs.get(name);
    }
}
