package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class StandaloneLoggerImpl extends SimpleTerminalConsole implements Logger {
    private final org.slf4j.Logger logger;
    private final String prefixString;

    public StandaloneLoggerImpl(org.slf4j.Logger logger) {
        this(logger, "");
    }

    public StandaloneLoggerImpl(org.slf4j.Logger logger, String prefixString) {
        this.logger = logger;
        this.prefixString = prefixString;
    }

    @Override
    public void info(String message) { logger.info(prefix(message)); }
    @Override
    public void warn(String message) { logger.warn(prefix(message)); }
    @Override
    public void error(String message) { logger.error(prefix(message)); }
    @Override
    public void error(String message, Throwable ex) { logger.error(prefix(message), ex); }
    @Override
    public void debug(String message) { logger.debug(prefix(message)); }
    @Override
    public Logger prefixed(String prefixString) { return new StandaloneLoggerImpl(logger, prefixString); }

    private String prefix(String message) {
        return prefixString.isEmpty() ? message : "[" + prefixString + "] " + message;
    }

    public void setDebug(boolean debug) { Configurator.setLevel(logger.getName(), debug ? Level.DEBUG : Level.INFO); }

    @Override
    protected boolean isRunning() { return true; }

    @Override
    protected void runCommand(String command) {
        String[] args = command.split(" ");
        String commandNode = args[0].toLowerCase();
        try {
            switch (commandNode) {
                case "exit" -> System.exit(0);
                case "restart" -> StandaloneMain.restart();
                case "dumpsession" -> {
                    info("Dumping session responses to 'lastSessionResponse.json' and 'currentSessionResponse.json'");
                    StandaloneMain.sessionManager.dumpSession();
                }
                case "accounts" -> handleAccounts(args);
                case "session" -> handlePrimarySession(args);
                case "version" -> info("MCXboxBroadcast Standalone " + BuildData.VERSION);
                case "help" -> {
                    info("Available commands:");
                    info("exit - Exit the application");
                    info("restart - Restart the application");
                    info("dumpsession - Dump the current session to json files");
                    info("accounts list - List sub-accounts");
                    info("accounts add <sub-session-id> - Add a sub-account");
                    info("accounts remove <sub-session-id> - Remove a sub-account");
                    info("accounts set <sub-session-id> <hostName|worldName|players|maxPlayers|ip|port> <value> - Edit sub-session broadcast fields");
                    info("session set <hostName|worldName|players|maxPlayers|ip|port> <value> - Edit primary broadcast fields");
                    info("version - Display the version");
                }
                default -> warn("Unknown command: " + commandNode);
            }
        } catch (Exception e) {
            error("Failed to execute command", e);
        }
    }


    private void handlePrimarySession(String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("set")) {
            warn("Usage: session set <field> <value>");
            return;
        }
        applyField(StandaloneMain.sessionInfo, args[2], joinFrom(args, 3));
        info("Updated primary session field " + args[2]);
    }
    private void handleAccounts(String[] args) {
        if (args.length < 2) {
            warn("Usage:");
            warn("accounts list");
            warn("accounts add/remove <sub-session-id>");
            warn("accounts set <sub-session-id> <field> <value>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list" -> StandaloneMain.sessionManager.listSessions();
            case "add" -> StandaloneMain.sessionManager.addSubSession(args[2]);
            case "remove" -> StandaloneMain.sessionManager.removeSubSession(args[2]);
            case "set" -> {
                if (args.length < 5) {
                    warn("accounts set <sub-session-id> <field> <value>");
                    return;
                }
                SessionInfo info = StandaloneMain.sessionManager.sessionInfo().copy();
                applyField(info, args[3], joinFrom(args, 4));
                StandaloneMain.sessionManager.updateSubSessionInfo(args[2], info);
                info("Updated sub-session " + args[2] + " field " + args[3]);
            }
            default -> warn("Unknown accounts command: " + args[1]);
        }
    }

    private static String joinFrom(String[] args, int idx) {
        StringBuilder out = new StringBuilder();
        for (int i = idx; i < args.length; i++) {
            if (i > idx) out.append(' ');
            out.append(args[i]);
        }
        return out.toString();
    }

    private static void applyField(SessionInfo info, String field, String value) {
        switch (field) {
            case "hostName" -> info.setHostName(value);
            case "worldName" -> info.setWorldName(value);
            case "players" -> info.setPlayers(Integer.parseInt(value));
            case "maxPlayers" -> info.setMaxPlayers(Integer.parseInt(value));
            case "ip" -> info.setIp(value);
            case "port" -> info.setPort(Integer.parseInt(value));
            default -> throw new IllegalArgumentException("Unknown field " + field);
        }
    }

    @Override
    protected void shutdown() {}
}
