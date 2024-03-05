package org.xtreemes.voicechat.client.pipe;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Packet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xtreemes.voicechat.client.StartClientTick;
import org.xtreemes.voicechat.client.VoiceChatManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PipeListener implements IPCListener {
    private static final String redirect = "http://localhost:1500/api/auth/discord/redirect";

    @Override
    public void onPacketSent(IPCClient client, Packet packet){

    }

    @Override
    public void onPacketReceived(IPCClient client, Packet packet){
        JSONObject json = packet.getJson();

        if(json.has("cmd")){
            String cmd = json.getString("cmd");
            switch (cmd) {
                case "AUTHORIZE" -> {
                    JSONObject data = json.getJSONObject("data");
                    String code = data.get("code").toString();
                    HttpClient httpclient = HttpClients.createDefault();
                    List<NameValuePair> params = new ArrayList<NameValuePair>(5);
                    params.add(new BasicNameValuePair("grant_type", "authorization_code"));
                    params.add(new BasicNameValuePair("code", code));
                    params.add(new BasicNameValuePair("redirect_url", redirect));
                    params.add(new BasicNameValuePair("client_id", Long.toString(PipeManager.getClientID())));
                    params.add(new BasicNameValuePair("client_secret", "dOejuYv0J0_u3zhLC16mLlz6f5JD1EAD"));
                    HttpPost httppost = new HttpPost("https://discord.com/api/oauth2/token");
                    try {
                        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                        HttpResponse response = httpclient.execute(httppost);
                        HttpEntity entity = response.getEntity();

                        if (entity != null) {
                            try (InputStream instream = entity.getContent()) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
                                StringBuilder response_builder = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    response_builder.append(line);
                                }
                                String response_string = response_builder.toString();
                                code = new JSONObject(response_string).getString("access_token");

                                JSONObject authenticate = new JSONObject()
                                        .put("cmd", "AUTHENTICATE")
                                        .put("args", new JSONObject()
                                                .put("access_token", code)
                                                .put("redirect_uri", redirect));


                                PipeManager.sendThroughPipe(authenticate);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "AUTHENTICATE" -> {
                    System.out.println("Authenticate sent!");
                    JSONObject subscribe = new JSONObject()
                            .put("cmd", "SUBSCRIBE")
                            .put("evt", "VOICE_CHANNEL_SELECT");
                    ClientTickEvents.START_CLIENT_TICK.register(new StartClientTick());
                    PipeManager.sendThroughPipe(subscribe);
                }
                case "DISPATCH" -> {
                    String event = json.getString("evt");
                    if (event.equals("VOICE_CHANNEL_SELECT")) {
                        Object channel_id = json.getJSONObject("data").get("channel_id");
                        if(channel_id instanceof String) {
                            VoiceChatManager.setActiveVC(channel_id.toString());

                             JSONObject get_channel = new JSONObject()
                                    .put("cmd", "GET_CHANNEL")
                                    .put("args", new JSONObject()
                                            .put("channel_id", channel_id)
                                    );
                             PipeManager.sendThroughPipe(get_channel);
                        } else {
                            VoiceChatManager.setActiveVC(null);
                            VoiceChatManager.setVoiceChatState(false);
                        }
                    }
                }
                case "GET_CHANNEL" -> {
                    JSONObject data = json.getJSONObject("data");
                    if(data.has("name")) {
                        String name = data.getString("name");
                        boolean current = VoiceChatManager.getVoiceChatState();
                        boolean state = name.equals("Voice Chat");
                        if (state) {
                            JSONArray states = data.getJSONArray("voice_states");
                            for(int i = 0; i < states.length(); i++){
                                JSONObject json_object = states.getJSONObject(i);
                                String nick = json_object.getString("nick");
                                if(VoiceChatManager.getUser(nick) == -100){
                                    VoiceChatManager.setUser(nick, "0");
                                    VoiceChatManager.setDefaultVolume(nick, json_object.getInt("volume"));
                                    String user_id = json_object.getJSONObject("user").getString("id");
                                    VoiceChatManager.setUserID(nick, user_id);
                                }
                            }
                        }
                        if (state && !current) {
                            JSONArray states = data.getJSONArray("voice_states");
                            VoiceChatManager.setDefaultVolumes(states);
                        }
                        VoiceChatManager.setVoiceChatState(state);
                    } else {
                        VoiceChatManager.setVoiceChatState(false);
                    }
                }
            }
        }
    }
}
