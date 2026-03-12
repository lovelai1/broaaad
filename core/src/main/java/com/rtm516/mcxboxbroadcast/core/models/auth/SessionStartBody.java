package com.rtm516.mcxboxbroadcast.core.models.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public final class SessionStartBody {
    public static String create(String deviceId, String playfabSessionTicket) {
        JsonObject root = new JsonObject();

        JsonObject device = new JsonObject();
        device.addProperty("applicationType", "MinecraftPE");
        device.add("capabilities", new JsonArray());
        device.addProperty("gameVersion", "1.21.20");
        device.addProperty("id", deviceId);
        device.addProperty("memory", "8589934592");
        device.addProperty("platform", "Windows10");
        device.addProperty("playFabTitleId", "20CA2");
        device.addProperty("storePlatform", "uwp.store");
        device.add("treatmentOverrides", JsonNull.INSTANCE);
        device.addProperty("type", "Windows10");

        JsonObject user = new JsonObject();
        user.addProperty("language", "en");
        user.addProperty("languageCode", "en-US");
        user.addProperty("regionCode", "US");
        user.addProperty("token", playfabSessionTicket);
        user.addProperty("tokenType", "PlayFab");

        root.add("device", device);
        root.add("user", user);
        return root.toString();
    }
}
