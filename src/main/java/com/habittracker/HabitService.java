package com.habittracker;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HabitService {
    private final DataManager dataManager;
    private HabitData habitData;
    
    public HabitService() {
        this.dataManager = new DataManager();
        this.habitData = dataManager.loadData();
    }
    
    public boolean addHabit(String name, String description) {
        if (findHabitByName(name).isPresent()) {
            return false;
        }
        
        List<Habit> habits = new ArrayList<>(habitData.getHabits());
        habits.add(new Habit(name, description));
        
        habitData = new HabitData(habits, habitData.getLogs(), habitData.getAlertSettings());
        dataManager.saveData(habitData);
        return true;
    }
    
    public boolean deleteHabit(String name) {
        Optional<Habit> habit = findHabitByName(name);
        if (!habit.isPresent()) {
            return false;
        }
        
        List<Habit> habits = habitData.getHabits().stream()
            .filter(h -> !h.getName().equals(name))
            .collect(Collectors.toList());
        
        List<HabitLog> logs = habitData.getLogs().stream()
            .filter(log -> !log.getHabitName().equals(name))
            .collect(Collectors.toList());
        
        habitData = new HabitData(habits, logs, habitData.getAlertSettings());
        dataManager.saveData(habitData);
        return true;
    }
    
    public List<Habit> listHabits() {
        return new ArrayList<>(habitData.getHabits());
    }
    
    public boolean logHabit(String name, LocalDate date) {
        if (!findHabitByName(name).isPresent()) {
            return false;
        }
        
        List<HabitLog> logs = new ArrayList<>(habitData.getLogs());
        logs.removeIf(log -> log.getHabitName().equals(name) && log.getDate().equals(date));
        logs.add(new HabitLog(name, date));
        
        habitData = new HabitData(habitData.getHabits(), logs, habitData.getAlertSettings());
        dataManager.saveData(habitData);
        return true;
    }
    
    public void setAlertTime(LocalTime time) {
        AlertSettings settings = new AlertSettings(time);
        habitData = habitData.withAlertSettings(settings);
        dataManager.saveData(habitData);
    }
    
    public void disableAlert() {
        if (habitData.getAlertSettings() != null) {
            AlertSettings settings = habitData.getAlertSettings().withEnabled(false);
            habitData = habitData.withAlertSettings(settings);
            dataManager.saveData(habitData);
        }
    }
    
    public Optional<AlertSettings> getAlertSettings() {
        return Optional.ofNullable(habitData.getAlertSettings());
    }
    
    private Optional<Habit> findHabitByName(String name) {
        return habitData.getHabits().stream()
            .filter(habit -> habit.getName().equals(name))
            .findFirst();
    }
    
    public List<HabitLog> getLogsForHabit(String habitName) {
        return habitData.getLogs().stream()
            .filter(log -> log.getHabitName().equals(habitName))
            .collect(Collectors.toList());
    }
    
    public boolean isHabitLoggedForDate(String habitName, LocalDate date) {
        return habitData.getLogs().stream()
            .anyMatch(log -> log.getHabitName().equals(habitName) && 
                           log.getDate().equals(date) && 
                           log.isCompleted());
    }
    
    public int getCurrentStreak(String habitName, LocalDate referenceDate) {
        return StreakCalculator.calculateCurrentStreak(habitName, habitData.getLogs(), referenceDate);
    }
    
    public int getLongestStreak(String habitName) {
        return StreakCalculator.calculateLongestStreak(habitName, habitData.getLogs());
    }
}