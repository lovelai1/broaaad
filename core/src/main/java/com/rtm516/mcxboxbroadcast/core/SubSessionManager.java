package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public class SubSessionManager extends SessionManagerCore {
    private final SessionManager parent;

    /**
     * Create a new session manager for a sub-session
     *
     * @param id The id of the sub-session
     * @param parent The parent session manager
     * @param storageManager The storage manager to use for storing data
     * @param notificationManager The notification manager to use for sending messages
     * @param logger The logger to use for outputting messages
     */
    public SubSessionManager(String id, SessionManager parent, StorageManager storageManager, NotificationManager notificationManager, Logger logger) {
        super(storageManager, notificationManager, logger.prefixed("Sub-Session " + id));
        this.parent = parent;

        // Sub-sessions now behave like primary sessions:
        // they keep their own session id and update their own CREATE_SESSION endpoint.
        this.sessionInfo = new ExpandedSessionInfo("", "", parent.sessionInfo().copy());
    }

    @Override
    public ScheduledExecutorService scheduledThread() {
        return parent.scheduledThread();
    }

    @Override
    public String getSessionId() {
        return sessionInfo.getSessionId();
    }

    @Override
    protected boolean handleFriendship() {
        // Treat sub-sessions the same as primary sessions.
        return false;
    }

    @Override
    public boolean socialActionsReady() {
        return parent.socialActionsReady();
    }

    @Override
    protected void updateSession() throws SessionUpdateException {
        // Make sure the websocket connection is still active
        checkConnection();

        super.updateSessionInternal(
            Constants.CREATE_SESSION.formatted(this.sessionInfo.getSessionId()),
            new CreateSessionRequest(this.sessionInfo)
        );
    }
}
