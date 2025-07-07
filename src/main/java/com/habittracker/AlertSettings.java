package com.habittracker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalTime;

public class AlertSettings {
    private final LocalTime alertTime;
    private final boolean enabled;
    
    @JsonCreator
    public AlertSettings(@JsonProperty("alertTime") LocalTime alertTime,
                        @JsonProperty("enabled") boolean enabled) {
        this.alertTime = alertTime;
        this.enabled = enabled;
    }
    
    public AlertSettings(LocalTime alertTime) {
        this(alertTime, true);
    }
    
    public LocalTime getAlertTime() {
        return alertTime;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public AlertSettings withEnabled(boolean enabled) {
        return new AlertSettings(this.alertTime, enabled);
    }
    
    @Override
    public String toString() {
        return String.format("AlertSettings{time=%s, enabled=%s}", 
                           alertTime, enabled);
    }
}