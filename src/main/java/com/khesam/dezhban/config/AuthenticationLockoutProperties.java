package com.khesam.dezhban.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "security.authentication-lockout")
public class AuthenticationLockoutProperties {

    private int userMaxAttempts = 5;
    private Duration userObservationWindow = Duration.ofMinutes(15);
    private Duration userLockDuration = Duration.ofMinutes(15);
    private int clientMaxAttempts = 10;
    private Duration clientObservationWindow = Duration.ofMinutes(10);
    private Duration clientLockDuration = Duration.ofMinutes(10);

    public int getUserMaxAttempts() {
        return userMaxAttempts;
    }

    public void setUserMaxAttempts(int userMaxAttempts) {
        this.userMaxAttempts = requirePositive(userMaxAttempts, "userMaxAttempts");
    }

    public Duration getUserObservationWindow() {
        return userObservationWindow;
    }

    public void setUserObservationWindow(Duration userObservationWindow) {
        this.userObservationWindow = requirePositive(userObservationWindow, "userObservationWindow");
    }

    public Duration getUserLockDuration() {
        return userLockDuration;
    }

    public void setUserLockDuration(Duration userLockDuration) {
        this.userLockDuration = requirePositive(userLockDuration, "userLockDuration");
    }

    public int getClientMaxAttempts() {
        return clientMaxAttempts;
    }

    public void setClientMaxAttempts(int clientMaxAttempts) {
        this.clientMaxAttempts = requirePositive(clientMaxAttempts, "clientMaxAttempts");
    }

    public Duration getClientObservationWindow() {
        return clientObservationWindow;
    }

    public void setClientObservationWindow(Duration clientObservationWindow) {
        this.clientObservationWindow =
                requirePositive(clientObservationWindow, "clientObservationWindow");
    }

    public Duration getClientLockDuration() {
        return clientLockDuration;
    }

    public void setClientLockDuration(Duration clientLockDuration) {
        this.clientLockDuration = requirePositive(clientLockDuration, "clientLockDuration");
    }

    private int requirePositive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero");
        }
        return value;
    }

    private Duration requirePositive(Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero");
        }
        return value;
    }
}
