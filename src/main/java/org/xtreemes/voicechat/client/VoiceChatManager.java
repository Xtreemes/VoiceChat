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
    private static final HashMap<String, String> reverseUserIDs = new HashMap<>();

    // nickname, default volume
    private static final HashMap<String, Integer> default_volumes = new HashMap<>();
    private static boolean is_in_voicechat = false;
    private static boolean thread_active = false;

    public static String getActiveVC() {
        return active_vc;
    }

    public static void setVoiceChatState(boolean state){
        if(!state && is_in_voicechat){
            if(!thread_active) {
                thread_active = true;
                HashMap<String, Integer> default_volumes_copy = new HashMap<>(default_volumes);
                HashMap<String, String> userIDs_copy = new HashMap<>(userIDs);
                HashMap<String, Integer> users_copy = new HashMap<>(users);

                Thread reset_volumes = new Thread(() -> {
                    try {
                        Thread.sleep(1000);

                        for (String name : users_copy.keySet()) {
                            int default_volume = default_volumes_copy.get(name);
                            String user_id = userIDs_copy.get(name);
                            System.out.println("Set " + name + " volume to " + default_volume);

                            if (default_volume < 100) {
                                default_volume = ((-1200 / (default_volume - 111)) - 11);
                            }
                            default_volume = Math.max(Math.min(default_volume, 200), 0);

                            JSONObject volume_json = new JSONObject()
                                    .put("cmd", "SET_USER_VOICE_SETTINGS")
                                    .put("args", new JSONObject()
                                            .put("user_id", user_id)
                                            .put("volume", default_volume));
                            PipeManager.sendThroughPipe(volume_json);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    thread_active = false;
                });
                reset_volumes.start();
            }

            users.clear();
            default_volumes.clear();
            userIDs.clear();
        }
        is_in_voicechat = state;
    }
    public static HashMap<String, String> getUsers() { return userIDs; }
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
    public static void setDefaultVolume(String nick, Integer v){
        default_volumes.computeIfAbsent(nick, key -> {
                    int volume = v;
                    return volume < 100 ? (-1200 / (volume + 11)) + 111 : volume;
                });
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
        reverseUserIDs.put(id, name);
    }
    public static String getUserID(String name){
        return userIDs.get(name);
    }
    public static String getFromID(String id){
        return reverseUserIDs.get(id);
    }
}
