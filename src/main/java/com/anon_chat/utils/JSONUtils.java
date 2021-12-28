package com.anon_chat.utils;

import org.json.JSONObject;

public class JSONUtils {
    public static String createRequest(String operation) {
        JSONObject object = new JSONObject();
        object.put("operation", operation);
        return object.toString();
    }

    public static String createRequest(String operation, String data) {
        JSONObject object = new JSONObject();
        object.put("operation", operation);
        object.put("data", data);
        return object.toString();
    }
}
