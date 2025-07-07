package com.habittracker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class HabitLog {
    private final String habitName;
    private final LocalDate date;
    private final boolean completed;
    
    @JsonCreator
    public HabitLog(@JsonProperty("habitName") String habitName,
                    @JsonProperty("date") LocalDate date,
                    @JsonProperty("completed") boolean completed) {
        this.habitName = habitName;
        this.date = date;
        this.completed = completed;
    }
    
    public HabitLog(String habitName, LocalDate date) {
        this(habitName, date, true);
    }
    
    public String getHabitName() {
        return habitName;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    @Override
    public String toString() {
        return String.format("HabitLog{habit='%s', date=%s, completed=%s}", 
                           habitName, date, completed);
    }
}