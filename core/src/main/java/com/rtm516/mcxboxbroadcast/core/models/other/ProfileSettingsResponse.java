package com.rtm516.mcxboxbroadcast.core.models.other;

import java.util.List;

public record ProfileSettingsResponse(List<ProfileUser> profileUsers) {
    public record ProfileUser(
        String hostId,
        String id,
        boolean isSponsoredUser,
        List<ProfileSetting> settings
    ) { }

    public record ProfileSetting(
        String id,
        String value
    ) { }

    public String gamertag() {
        if (profileUsers == null || profileUsers.isEmpty() || profileUsers.get(0).settings() == null) {
            return null;
        }
        for (ProfileSetting setting : profileUsers.get(0).settings()) {
            if ("Gamertag".equalsIgnoreCase(setting.id()) || "GamertagUnique".equalsIgnoreCase(setting.id())) {
                return setting.value();
            }
        }
        return null;
    }

    public String xuid() {
        if (profileUsers == null || profileUsers.isEmpty()) {
            return null;
        }
        String id = profileUsers.get(0).id();
        if (id == null) {
            return null;
        }
        if (id.startsWith("xuid(")) {
            int end = id.indexOf(')');
            if (end > 5) {
                return id.substring(5, end);
            }
        }
        return id;
    }
}
