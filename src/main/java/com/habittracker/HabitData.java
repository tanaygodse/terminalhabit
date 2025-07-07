package com.habittracker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class HabitData {
    private final List<Habit> habits;
    private final List<HabitLog> logs;
    private final AlertSettings alertSettings;
    
    @JsonCreator
    public HabitData(@JsonProperty("habits") List<Habit> habits,
                     @JsonProperty("logs") List<HabitLog> logs,
                     @JsonProperty("alertSettings") AlertSettings alertSettings) {
        this.habits = habits != null ? habits : new ArrayList<>();
        this.logs = logs != null ? logs : new ArrayList<>();
        this.alertSettings = alertSettings;
    }
    
    public HabitData() {
        this(new ArrayList<>(), new ArrayList<>(), null);
    }
    
    public List<Habit> getHabits() {
        return habits;
    }
    
    public List<HabitLog> getLogs() {
        return logs;
    }
    
    public AlertSettings getAlertSettings() {
        return alertSettings;
    }
    
    public HabitData withAlertSettings(AlertSettings alertSettings) {
        return new HabitData(this.habits, this.logs, alertSettings);
    }
}