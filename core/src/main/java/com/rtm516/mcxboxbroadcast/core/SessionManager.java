package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonParseException;
import com.rtm516.mcxboxbroadcast.core.configs.CoreConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateSessionResponse;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;
import org.java_websocket.util.NamedThreadFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SessionManager extends SessionManagerCore {
    private final ScheduledExecutorService scheduledThreadPool;
    private final Map<String, SubSessionManager> subSessionManagers;
    private final Map<String, SessionInfo> subSessionConfig;

    private CoreConfig.FriendSyncConfig friendSyncConfig;
    private Runnable restartCallback;

    public SessionManager(StorageManager storageManager, NotificationManager notificationManager, Logger logger) {
        super(storageManager, notificationManager, logger.prefixed("Primary Session"));
        this.scheduledThreadPool = Executors.newScheduledThreadPool(5, new NamedThreadFactory("MCXboxBroadcast Thread"));
        this.subSessionManagers = new HashMap<>();
        this.subSessionConfig = new HashMap<>();
    }

    @Override
    public ScheduledExecutorService scheduledThread() {
        return scheduledThreadPool;
    }

    @Override
    public String getSessionId() {
        return sessionInfo.getSessionId();
    }

    public ExpandedSessionInfo sessionInfo() {
        return sessionInfo;
    }

    public void init(SessionInfo sessionInfo, CoreConfig.FriendSyncConfig friendSyncConfig) throws SessionCreationException, SessionUpdateException {
        this.sessionInfo = new ExpandedSessionInfo("", "", sessionInfo);

        super.init();

        this.friendSyncConfig = friendSyncConfig;
        friendManager().init(this.friendSyncConfig);

        List<String> subSessions = new ArrayList<>();
        try {
            String subSessionsJson = storageManager().subSessions();
            if (!subSessionsJson.isBlank()) {
                subSessions = Arrays.asList(Constants.GSON.fromJson(subSessionsJson, String[].class));
            }
        } catch (IOException ignored) { }

        List<String> finalSubSessions = subSessions;
        scheduledThreadPool.execute(() -> {
            for (String subSession : finalSubSessions) {
                try {
                    createAndInitSubSession(subSession, getSubSessionConfig(subSession));
                } catch (SessionCreationException | SessionUpdateException e) {
                    logger.error("Failed to create sub-session " + subSession, e);
                }
            }
        });
    }

    @Override
    protected boolean handleFriendship() {
        return false;
    }

    public void updateSession(SessionInfo sessionInfo) throws SessionUpdateException {
        this.sessionInfo.updateSessionInfo(sessionInfo);
        updateSession();

        for (Map.Entry<String, SubSessionManager> entry : subSessionManagers.entrySet()) {
            SessionInfo info = getSubSessionConfig(entry.getKey());
            if (info.getHostName().isEmpty()) {
                info.setHostName(entry.getValue().getXboxToken().gamertag());
            }
            entry.getValue().updateSession(info);
        }
    }

    @Override
    protected void updateSession() throws SessionUpdateException {
        checkConnection();

        String responseBody = super.updateSessionInternal(Constants.CREATE_SESSION.formatted(this.sessionInfo.getSessionId()), new CreateSessionRequest(this.sessionInfo));
        try {
            CreateSessionResponse sessionResponse = Constants.GSON.fromJson(responseBody, CreateSessionResponse.class);

            int players = sessionResponse.members().size();
            if (players >= 28) {
                logger.info("Restarting session due to " + players + "/30 players");
                restart();
            }
        } catch (JsonParseException e) {
            throw new SessionUpdateException("Failed to parse session response: " + e.getMessage());
        }
    }

    public void shutdown() {
        for (SubSessionManager subSessionManager : subSessionManagers.values()) {
            subSessionManager.shutdown();
        }

        super.shutdown();
        scheduledThreadPool.shutdownNow();
    }

    public void dumpSession() {
        try {
            storageManager().lastSessionResponse(lastSessionResponse);
        } catch (IOException e) {
            logger.error("Error dumping last session: " + e.getMessage());
        }

        HttpRequest createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.CREATE_SESSION.formatted(this.sessionInfo.getSessionId())))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .GET()
                .build();

        try {
            HttpResponse<String> createSessionResponse = httpClient.send(createSessionRequest, HttpResponse.BodyHandlers.ofString());

            storageManager().currentSessionResponse(createSessionResponse.body());
        } catch (IOException | InterruptedException e) {
            logger.error("Error dumping current session: " + e.getMessage());
        }
    }

    public void addSubSession(String id) {
        if (subSessionManagers.containsKey(id)) {
            coreLogger.error("Sub-session already exists with that ID");
            return;
        }

        try {
            createAndInitSubSession(id, this.sessionInfo.copy());
        } catch (SessionCreationException | SessionUpdateException e) {
            coreLogger.error("Failed to create sub-session", e);
            return;
        }

        updateSubSessionList();
    }

    private void createAndInitSubSession(String id, SessionInfo info) throws SessionCreationException, SessionUpdateException {
        SubSessionManager subSessionManager = new SubSessionManager(id, this, info, storageManager().subSession(id), notificationManager(), logger);
        subSessionManager.init();
        subSessionManager.friendManager().init(this.friendSyncConfig);
        subSessionManagers.put(id, subSessionManager);
        subSessionConfig.put(id, info);
        saveSubSessionConfig(id, info);
    }

    public void removeSubSession(String id) {
        if (!subSessionManagers.containsKey(id)) {
            coreLogger.error("Sub-session does not exist with that ID");
            return;
        }

        subSessionManagers.get(id).shutdown();
        subSessionManagers.remove(id);
        subSessionConfig.remove(id);

        try {
            storageManager().subSession(id).cleanup();
        } catch (IOException e) {
            coreLogger.error("Failed to delete sub-session cache file", e);
        }

        updateSubSessionList();
        coreLogger.info("Removed sub-session with ID " + id);
    }

    private void updateSubSessionList() {
        try {
            storageManager().subSessions(Constants.GSON.toJson(subSessionManagers.keySet()));
        } catch (JsonParseException | IOException e) {
            coreLogger.error("Failed to update sub-session list", e);
        }
    }

    public void updateSubSessionInfo(String id, SessionInfo info) {
        if (!subSessionManagers.containsKey(id)) {
            coreLogger.error("Sub-session does not exist with that ID");
            return;
        }
        subSessionConfig.put(id, info);
        saveSubSessionConfig(id, info);
    }

    private void saveSubSessionConfig(String id, SessionInfo info) {
        try {
            storageManager().subSession(id).sessionInfo(Constants.GSON.toJson(info));
        } catch (IOException e) {
            logger.error("Failed to save sub-session session info for " + id, e);
        }
    }

    private SessionInfo getSubSessionConfig(String id) {
        if (subSessionConfig.containsKey(id)) {
            return subSessionConfig.get(id).copy();
        }

        try {
            String raw = storageManager().subSession(id).sessionInfo();
            if (!raw.isBlank()) {
                SessionInfo loaded = Constants.GSON.fromJson(raw, SessionInfo.class);
                subSessionConfig.put(id, loaded);
                return loaded.copy();
            }
        } catch (Exception ignored) {}

        SessionInfo fallback = this.sessionInfo.copy();
        subSessionConfig.put(id, fallback);
        return fallback.copy();
    }

    public void listSessions() {
        List<String> messages = new ArrayList<>();
        coreLogger.info("Loading status of sessions...");

        messages.add("Primary Session:");
        messages.add(" - Gamertag: " + getXboxToken().gamertag());
        messages.add("   Following: " + socialSummary().targetFollowingCount() + "/" + Constants.MAX_FRIENDS);

        if (!subSessionManagers.isEmpty()) {
            messages.add("Sub-sessions: (" + subSessionManagers.size() + ")");
            for (Map.Entry<String, SubSessionManager> subSession : subSessionManagers.entrySet()) {
                SessionInfo cfg = getSubSessionConfig(subSession.getKey());
                messages.add(" - ID: " + subSession.getKey());
                messages.add("   Gamertag: " + subSession.getValue().getXboxToken().gamertag());
                messages.add("   Broadcast Host: " + cfg.getHostName());
                messages.add("   Broadcast World: " + cfg.getWorldName());
                messages.add("   Following: " + subSession.getValue().socialSummary().targetFollowingCount() + "/" + Constants.MAX_FRIENDS);
            }
        } else {
            messages.add("No sub-sessions");
        }

        for (String message : messages) {
            coreLogger.info(message);
        }
    }

    public void restartCallback(Runnable restart) {
        this.restartCallback = restart;
    }

    public void restart() {
        if (restartCallback != null) {
            restartCallback.run();
        } else {
            logger.error("No restart callback set");
        }
    }

    public String getGamertag() {
        return getXboxToken().gamertag();
    }

    public String getXuid() {
        return getXboxToken().userXUID();
    }
}
