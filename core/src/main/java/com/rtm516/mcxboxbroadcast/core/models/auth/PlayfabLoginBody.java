package com.rtm516.mcxboxbroadcast.core.models.auth;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public final class PlayfabLoginBody {
    public static JsonObject create(String xboxToken) {
        JsonObject root = new JsonObject();
        root.addProperty("CreateAccount", true);
        root.add("EncryptedRequest", JsonNull.INSTANCE);

        JsonObject info = new JsonObject();
        info.addProperty("GetCharacterInventories", false);
        info.addProperty("GetCharacterList", false);
        info.addProperty("GetPlayerProfile", true);
        info.addProperty("GetPlayerStatistics", false);
        info.addProperty("GetTitleData", false);
        info.addProperty("GetUserAccountInfo", true);
        info.addProperty("GetUserData", false);
        info.addProperty("GetUserInventory", false);
        info.addProperty("GetUserReadOnlyData", false);
        info.addProperty("GetUserVirtualCurrency", false);
        info.add("PlayerStatisticNames", JsonNull.INSTANCE);
        info.add("ProfileConstraints", JsonNull.INSTANCE);
        info.add("TitleDataKeys", JsonNull.INSTANCE);
        info.add("UserDataKeys", JsonNull.INSTANCE);
        info.add("UserReadOnlyDataKeys", JsonNull.INSTANCE);
        root.add("InfoRequestParameters", info);

        root.add("PlayerSecret", JsonNull.INSTANCE);
        root.addProperty("TitleId", "20CA2");
        root.addProperty("XboxToken", "XBL3.0 x=" + xboxToken);
        return root;
    }
}
