package com.habittracker;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class StreakCalculator {
    
    public static int calculateCurrentStreak(String habitName, List<HabitLog> logs, LocalDate referenceDate) {
        List<LocalDate> completedDates = logs.stream()
            .filter(log -> log.getHabitName().equals(habitName) && log.isCompleted())
            .map(HabitLog::getDate)
            .sorted()
            .collect(Collectors.toList());
        
        if (completedDates.isEmpty()) {
            return 0;
        }
        
        int streak = 0;
        LocalDate currentDate = referenceDate;
        
        while (completedDates.contains(currentDate)) {
            streak++;
            currentDate = currentDate.minusDays(1);
        }
        
        return streak;
    }
    
    public static int calculateLongestStreak(String habitName, List<HabitLog> logs) {
        List<LocalDate> completedDates = logs.stream()
            .filter(log -> log.getHabitName().equals(habitName) && log.isCompleted())
            .map(HabitLog::getDate)
            .sorted()
            .collect(Collectors.toList());
        
        if (completedDates.isEmpty()) {
            return 0;
        }
        
        int longestStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < completedDates.size(); i++) {
            LocalDate prevDate = completedDates.get(i - 1);
            LocalDate currentDate = completedDates.get(i);
            
            if (currentDate.equals(prevDate.plusDays(1))) {
                currentStreak++;
            } else {
                longestStreak = Math.max(longestStreak, currentStreak);
                currentStreak = 1;
            }
        }
        
        return Math.max(longestStreak, currentStreak);
    }
}