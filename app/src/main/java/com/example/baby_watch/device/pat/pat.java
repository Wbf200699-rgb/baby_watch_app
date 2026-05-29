package com.example.baby_watch.device.pat;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.baby_watch.data.ip.ip_ok;
import com.example.baby_watch.data.send.send_json;

import java.util.HashMap;
import java.util.Map;

public class pat {

    private static final String PREFS_NAME = "pat_prefs";
    private static final String KEY_ENABLED = "pat_enabled";

    public static void sendPatCommand(boolean turnOn) {
        Map<String, Object> data = new HashMap<>();
        String cmd = turnOn ? "打开拍一拍" : "关闭拍一拍";
        data.put("command", cmd);
        send_json.replyToSender(data);
        com.example.baby_watch.service.log.LogManager.notification("轻拍安抚", cmd);
    }

    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
