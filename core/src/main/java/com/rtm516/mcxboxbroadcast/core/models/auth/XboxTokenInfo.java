package com.rtm516.mcxboxbroadcast.core.models.auth;

import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;

public record XboxTokenInfo(
    String userXUID,
    String userHash,
    String gamertag,
    String XSTSToken,
    String expiresOn) {

    public XboxTokenInfo(String userXUID, String gamertag, StepXblSisuAuthentication.XblSisuTokens xboxToken) {
        this(userXUID, xboxToken.getUserHash(), gamertag, xboxToken.getToken(), String.valueOf(xboxToken.getExpireTimeMs()));
    }

    public String tokenHeader() {
        return "XBL3.0 x=" + this.userHash + ";" + this.XSTSToken;
    }
}
