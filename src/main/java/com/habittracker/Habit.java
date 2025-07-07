package com.habittracker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class Habit {
    private final String name;
    private final LocalDate createdDate;
    private final String description;
    
    @JsonCreator
    public Habit(@JsonProperty("name") String name,
                 @JsonProperty("createdDate") LocalDate createdDate,
                 @JsonProperty("description") String description) {
        this.name = name;
        this.createdDate = createdDate;
        this.description = description;
    }
    
    public Habit(String name, String description) {
        this(name, LocalDate.now(), description);
    }
    
    public String getName() {
        return name;
    }
    
    public LocalDate getCreatedDate() {
        return createdDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return String.format("Habit{name='%s', created=%s, description='%s'}", 
                           name, createdDate, description);
    }
}