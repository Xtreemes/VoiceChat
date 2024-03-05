package org.xtreemes.voicechat.client.pipe;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.entities.pipe.Pipe;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

public class PipeManager {
    private static final HashMap<String, Callback> callbacks = new HashMap<>();
    private static volatile Pipe pipe;
    private static long client_id;
    private static Thread readThread = null;
    private static IPCListener listener = new PipeListener();
    private static IPCClient client;

    public static void setClientID(long id){
        client_id = id;
    }
    public static long getClientID(){
        return client_id;
    }

    public static void initializePipe() {
        callbacks.clear();

        client = new IPCClient(client_id);
        try {
            pipe = Pipe.openPipe(client, client_id, callbacks);
            listener.onReady(client);
            pipe.setListener(listener);

            Thread read_thread = new Thread(() -> {
                try {
                    while(true) {
                        pipe.setStatus(PipeStatus.CONNECTED);
                        pipe.read();
                    }
                } catch (IOException e){
                    throw new RuntimeException(e);
                }
            }, "IPCClient-Reader");
            read_thread.setDaemon(true);
            read_thread.start();


            //startReading();

            JSONObject authorize = new JSONObject()
                    .put("cmd","AUTHORIZE")
                    .put("args", new JSONObject()
                            .put("client_id", Long.toString(client_id))
                            .put("scopes", "rpc rpc.voice.write rpc.voice.read")
                    );


            pipe.send(Packet.OpCode.FRAME, authorize, new Callback());

        } catch (NoDiscordClientException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendThroughPipe(JSONObject json){
        pipe.send(Packet.OpCode.FRAME, json, new Callback());
    }

    // Stolen code lmao
    private static void startReading(){
        readThread = new Thread(() -> {
            try
            {
                Packet p;
                Packet.OpCode test = pipe.read().getOp();

                while((p = pipe.read()).getOp() != Packet.OpCode.CLOSE) {
                    JSONObject json = p.getJson();
                    String search_for = json.optString("evt", null);
                    if (search_for != null) {

                        IPCClient.Event event = IPCClient.Event.valueOf(search_for);
                        String nonce = json.optString("nonce", null);
                        switch (event) {
                            case NULL:
                                if (nonce != null && callbacks.containsKey(nonce))
                                    callbacks.remove(nonce).succeed(p);
                                break;

                            case ERROR:
                                if (nonce != null && callbacks.containsKey(nonce))
                                    callbacks.remove(nonce).fail(json.getJSONObject("data").optString("message", null));
                                break;

                        }
                        if (listener != null && json.has("cmd") && json.getString("cmd").equals("DISPATCH")) {
                            try {
                                JSONObject data = json.getJSONObject("data");
                                switch (IPCClient.Event.valueOf(json.getString("evt"))) {
                                    case ACTIVITY_JOIN:
                                        listener.onActivityJoin(client, data.getString("secret"));
                                        break;

                                    case ACTIVITY_SPECTATE:
                                        listener.onActivitySpectate(client, data.getString("secret"));
                                        break;

                                    case ACTIVITY_JOIN_REQUEST:
                                        JSONObject u = data.getJSONObject("user");
                                        User user = new User(
                                                u.getString("username"),
                                                u.getString("discriminator"),
                                                Long.parseLong(u.getString("id")),
                                                u.optString("avatar", null)
                                        );
                                        listener.onActivityJoinRequest(client, data.optString("secret", null), user);
                                        break;
                                }
                            } catch (Exception e) {
                                System.out.println("uh oh 1 " + e);
                            }
                        }
                    }
                    pipe.setStatus(PipeStatus.DISCONNECTED);
                    if (listener != null)
                        listener.onClose(client, p.getJson());
                }
            }
            catch(IOException | JSONException ex)
            {
                if(ex instanceof IOException)
                    System.out.println("uh oh 2 " + ex);
                else
                    System.out.println("uh oh 3 " + ex);

                pipe.setStatus(PipeStatus.DISCONNECTED);
                if(listener != null)
                    listener.onDisconnect(client, ex);
                throw new RuntimeException(ex);
            }
        });
        readThread.start();
    }
}
