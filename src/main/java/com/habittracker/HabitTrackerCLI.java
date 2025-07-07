package com.habittracker;

import java.awt.SystemTray;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class HabitTrackerCLI {
    private final HabitService habitService;
    private final NotificationService notificationService;
    
    public HabitTrackerCLI() {
        this.habitService = new HabitService();
        this.notificationService = new NotificationService(habitService);
    }
    
    public static void main(String[] args) {
        HabitTrackerCLI cli = new HabitTrackerCLI();
        
        if (args.length == 0) {
            cli.showHelp();
            return;
        }
        
        try {
            cli.processCommand(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void processCommand(String[] args) {
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "add":
                handleAddCommand(args);
                break;
            case "delete":
                handleDeleteCommand(args);
                break;
            case "list":
                handleListCommand();
                break;
            case "log":
                handleLogCommand(args);
                break;
            case "status":
                handleStatusCommand(args);
                break;
            case "set-alert":
                handleSetAlertCommand(args);
                break;
            case "disable-alert":
                handleDisableAlertCommand();
                break;
            case "daemon":
                handleDaemonCommand();
                break;
            case "test-notification":
                handleTestNotificationCommand();
                break;
            case "help":
                showHelp();
                break;
            default:
                throw new IllegalArgumentException("Unknown command: " + command);
        }
    }
    
    private void handleAddCommand(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: add <habit-name> [description]");
        }
        
        String name = args[1];
        String description = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";
        
        if (habitService.addHabit(name, description)) {
            System.out.println("✓ Added habit: " + name);
        } else {
            System.err.println("✗ Habit already exists: " + name);
        }
    }
    
    private void handleDeleteCommand(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: delete <habit-name>");
        }
        
        String name = args[1];
        if (habitService.deleteHabit(name)) {
            System.out.println("✓ Deleted habit: " + name);
        } else {
            System.err.println("✗ Habit not found: " + name);
        }
    }
    
    private void handleListCommand() {
        List<Habit> habits = habitService.listHabits();
        
        if (habits.isEmpty()) {
            System.out.println("No habits tracked yet. Use 'add' to create your first habit.");
            return;
        }
        
        System.out.println("Tracked Habits:");
        for (Habit habit : habits) {
            System.out.printf("• %s", habit.getName());
            if (!habit.getDescription().isEmpty()) {
                System.out.printf(" - %s", habit.getDescription());
            }
            System.out.printf(" (created: %s)%n", habit.getCreatedDate());
        }
    }
    
    private void handleLogCommand(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: log <habit-name> [date]");
        }
        
        String name = args[1];
        LocalDate date = LocalDate.now();
        
        if (args.length > 2) {
            try {
                date = LocalDate.parse(args[2]);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD");
            }
        }
        
        if (habitService.logHabit(name, date)) {
            int streak = habitService.getCurrentStreak(name, date);
            System.out.printf("✓ Logged habit '%s' for %s%n", name, date);
            System.out.printf("Current streak: %d day%s%n", streak, streak == 1 ? "" : "s");
        } else {
            System.err.println("✗ Habit not found: " + name);
        }
    }
    
    private void handleStatusCommand(String[] args) {
        LocalDate date = LocalDate.now();
        
        if (args.length > 1) {
            try {
                date = LocalDate.parse(args[1]);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD");
            }
        }
        
        List<Habit> habits = habitService.listHabits();
        
        if (habits.isEmpty()) {
            System.out.println("No habits tracked yet.");
            return;
        }
        
        System.out.printf("Habit Status for %s:%n", date);
        for (Habit habit : habits) {
            boolean logged = habitService.isHabitLoggedForDate(habit.getName(), date);
            int currentStreak = habitService.getCurrentStreak(habit.getName(), date);
            int longestStreak = habitService.getLongestStreak(habit.getName());
            
            System.out.printf("• %s: %s (streak: %d, best: %d)%n", 
                             habit.getName(), 
                             logged ? "✓" : "✗", 
                             currentStreak, 
                             longestStreak);
        }
    }
    
    private void handleSetAlertCommand(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: set-alert <time> (format: HH:MM)");
        }
        
        try {
            LocalTime time = LocalTime.parse(args[1], DateTimeFormatter.ofPattern("HH:mm"));
            habitService.setAlertTime(time);
            notificationService.startNotifications(true); // Keep JVM alive for background notifications
            System.out.printf("✓ Alert set for %s - running in background%n", time);
            System.out.println("Press Ctrl+C to stop background notifications");
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nStopping notifications...");
                notificationService.stopNotifications();
            }));
            
            try {
                Thread.currentThread().join(); // Keep main thread alive
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Use HH:MM (24-hour format)");
        }
    }
    
    private void handleDisableAlertCommand() {
        habitService.disableAlert();
        notificationService.stopNotifications();
        System.out.println("✓ Alerts disabled");
    }
    
    private void handleDaemonCommand() {
        Optional<AlertSettings> alertSettings = habitService.getAlertSettings();
        if (alertSettings.isPresent()) {
            AlertSettings settings = alertSettings.get();
            if (settings.isEnabled()) {
                System.out.printf("Starting daemon mode with alert at %s%n", settings.getAlertTime());
                System.out.println("Press Ctrl+C to stop...");
                
                notificationService.startNotifications(true);
                
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\nStopping notifications...");
                    notificationService.stopNotifications();
                }));
                
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                System.out.println("No alert configured or alerts are disabled. Use 'set-alert' first.");
            }
        } else {
            System.out.println("No alert configured. Use 'set-alert' first.");
        }
    }
    
    private void handleTestNotificationCommand() {
        System.out.println("Testing notification system...");
        System.out.println("SystemTray supported: " + SystemTray.isSupported());
        
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported on this platform");
            return;
        }
        
        try {
            notificationService.testNotification();
            System.out.println("Test notification sent successfully");
        } catch (Exception e) {
            System.err.println("Failed to send test notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showHelp() {
        System.out.println("Habit Tracker CLI");
        System.out.println("Commands:");
        System.out.println("  add <habit-name> [description]  - Add a new habit");
        System.out.println("  delete <habit-name>             - Delete a habit");
        System.out.println("  list                            - List all habits");
        System.out.println("  log <habit-name> [date]         - Log habit completion (default: today)");
        System.out.println("  status [date]                   - Show habit status (default: today)");
        System.out.println("  set-alert <time>                - Set daily reminder and run in background");
        System.out.println("  disable-alert                   - Disable notifications");
        System.out.println("  daemon                          - Run in background for notifications");
        System.out.println("  test-notification               - Test notification system");
        System.out.println("  help                            - Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar habit-tracker.jar add \"Morning Run\" \"Run for 30 minutes\"");
        System.out.println("  java -jar habit-tracker.jar log \"Morning Run\"");
        System.out.println("  java -jar habit-tracker.jar status");
        System.out.println("  java -jar habit-tracker.jar set-alert 19:30");
        System.out.println("  java -jar habit-tracker.jar daemon");
    }
}