package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import java.util.Base64;

public record AuthConfig(
        AuthType type,
        String username,
        String password,
        String token
) {
    public enum AuthType {
        NONE,
        BASIC,
        BEARER
    }

    public static AuthConfig none() {
        return new AuthConfig(AuthType.NONE, null, null, null);
    }

    public static AuthConfig basic(String username, String password) {
        return new AuthConfig(AuthType.BASIC, username, password, null);
    }

    public static AuthConfig bearer(String token) {
        return new AuthConfig(AuthType.BEARER, null, null, token);
    }

    public static AuthConfig fromConfig(TargetConfig config) {
        String authType = config.getString("auth_type", "none").toLowerCase();
        return switch (authType) {
            case "basic" -> basic(
                    config.getString("auth_username", ""),
                    config.getString("auth_password", "")
            );
            case "bearer" -> bearer(config.getString("auth_token", ""));
            default -> none();
        };
    }

    public String getAuthorizationHeader() {
        return switch (type) {
            case BASIC -> {
                String credentials = username + ":" + password;
                String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
                yield "Basic " + encoded;
            }
            case BEARER -> "Bearer " + token;
            case NONE -> null;
        };
    }

    public boolean hasAuth() {
        return type != AuthType.NONE;
    }
}
