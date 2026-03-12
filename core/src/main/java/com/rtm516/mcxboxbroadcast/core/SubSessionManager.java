package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.session.JoinSessionRequest;
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

        // Sub-sessions need their own connection/xuid context for JOIN_SESSION payload updates.
        // We seed with parent public session settings, but keep the same shared Xbox session id.
        this.sessionInfo = new ExpandedSessionInfo("", "", parent.sessionInfo().copy());
        this.sessionInfo.setSessionId(parent.sessionInfo().getSessionId());
    }

    @Override
    public ScheduledExecutorService scheduledThread() {
        return parent.scheduledThread();
    }

    @Override
    public String getSessionId() {
        return parent.sessionInfo().getSessionId();
    }

    @Override
    protected boolean handleFriendship() {
        // TODO Some form of force flag just in case the master friends list is full

        // Add the main account
        boolean subAdd = friendManager().addIfRequired(parent.getXboxToken().userXUID(), parent.getXboxToken().gamertag());

        // Get the main account to add us
        boolean mainAdd = parent.friendManager().addIfRequired(getXboxToken().userXUID(), getXboxToken().gamertag());

        return subAdd || mainAdd;
    }

    @Override
    public boolean socialActionsReady() {
        return parent.socialActionsReady();
    }

    @Override
    protected void updateSession() throws SessionUpdateException {
        if (this.sessionInfo == null || this.sessionInfo.getConnectionId() == null || this.sessionInfo.getConnectionId().isBlank()) {
            throw new SessionUpdateException("Sub-session is missing connection info and cannot join the parent session");
        }

        super.updateSessionInternal(
            Constants.JOIN_SESSION.formatted(parent.sessionInfo().getHandleId()),
            new JoinSessionRequest(this.sessionInfo)
        );
    }
}
