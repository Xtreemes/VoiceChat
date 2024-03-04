package org.xtreemes.voicechat.client;

public class VoiceChatManager {
    private static String active_vc = null;

    public static String getActiveVC() {
        return active_vc;
    }

    public static void setActiveVC(String vc) {
        active_vc = vc;
    }
}
