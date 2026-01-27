package dev.valhal.minecraft.plugin.EventNotifications.core.event;

public enum GameEventType {
    SERVER_STARTUP("server_startup"),
    SERVER_SHUTDOWN("server_shutdown"),
    PLAYER_CONNECT("player_connect"),
    PLAYER_DISCONNECT("player_disconnect"),
    PLAYER_DEATH("player_death"),
    PLAYER_BANNED("player_banned"),
    PLAYER_KICKED("player_kicked"),
    PLAYER_OP("player_op"),
    PLAYER_DEOP("player_deop"),
    PLAYER_WHITELISTED("player_whitelisted"),
    PLAYER_UNWHITELISTED("player_unwhitelisted"),
    PLAYER_PARDONED("player_pardoned"),
    SERVER_WHITELIST_ON("server_whitelist_on"),
    SERVER_WHITELIST_OFF("server_whitelist_off"),
    PLAYER_ADVANCEMENT("player_advancement");

    private final String configKey;

    GameEventType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
