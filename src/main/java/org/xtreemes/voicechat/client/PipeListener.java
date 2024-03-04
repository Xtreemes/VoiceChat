package org.xtreemes.voicechat.client;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Packet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.input.Input;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

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
        System.out.println("Packet received!");

        if(json.has("cmd")){
            String cmd = json.getString("cmd");
            System.out.println("CMD is: " + cmd);
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
                    ClientTickEvents.START_CLIENT_TICK.register(clientt -> {
                        System.out.println("test");
                    });
                    PipeManager.sendThroughPipe(subscribe);
                }
                case "DISPATCH" -> {
                    String event = json.getString("evt");
                    if (event.equals("VOICE_CHANNEL_SELECT")) {
                        System.out.println("Voice channel updated! New Channel: ");
                        Object channel_id = json.getJSONObject("data").get("channel_id");
                        if(channel_id instanceof String) {
                            VoiceChatManager.setActiveVC(channel_id.toString());

                            /* JSONObject get_channel = new JSONObject()
                                    .put("cmd", "GET_CHANNEL")
                                    .put("args", new JSONObject()
                                            .put("channel_id", channel_id)
                                    ); */
                        } else {
                            VoiceChatManager.setActiveVC(null);
                        }
                    }
                }
            }
        }
    }
}
