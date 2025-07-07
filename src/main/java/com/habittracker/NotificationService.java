package com.habittracker;

import java.awt.*;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class NotificationService {
    private Timer timer;
    private final HabitService habitService;
    
    public NotificationService(HabitService habitService) {
        this.habitService = habitService;
    }
    
    public void startNotifications() {
        startNotifications(false);
    }
    
    public void startNotifications(boolean keepAlive) {
        stopNotifications();
        
        habitService.getAlertSettings().ifPresent(settings -> {
            if (settings.isEnabled()) {
                scheduleNotification(settings.getAlertTime(), keepAlive);
            }
        });
    }
    
    public void stopNotifications() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    private void scheduleNotification(LocalTime alertTime, boolean keepAlive) {
        if (!SystemTray.isSupported()) {
            System.err.println("System tray is not supported on this platform");
            return;
        }
        
        if (keepAlive) {
            System.out.printf("Scheduling notifications for %s (checking every minute)%n", alertTime);
        }
        
        timer = new Timer(!keepAlive); // Use daemon timer unless keepAlive is true
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                if (now.getHour() == alertTime.getHour() && 
                    now.getMinute() == alertTime.getMinute()) {
                    System.out.printf("Alert time reached: %s - showing notification%n", now);
                    showNotification();
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));
    }
    
    private void showNotification() {
        int habitCount = habitService.listHabits().size();
        String message = String.format("Time to check your %d habit%s!", 
                                     habitCount, habitCount == 1 ? "" : "s");
        
        // Try AppleScript notification first (works better on macOS)
        if (showAppleScriptNotification(message)) {
            System.out.println("✓ Notification sent via AppleScript");
            return;
        }
        
        // Fallback to SystemTray notification
        if (showSystemTrayNotification(message)) {
            System.out.println("✓ Notification sent via SystemTray");
            return;
        }
        
        System.err.println("✗ Failed to send notification via any method");
    }
    
    private boolean showAppleScriptNotification(String message) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (!osName.contains("mac")) {
                return false; // Only works on macOS
            }
            
            System.out.println("Trying AppleScript notification...");
            
            String script = String.format(
                "display notification \"%s\" with title \"Habit Tracker Reminder\"", 
                message.replace("\"", "\\\"")
            );
            
            ProcessBuilder pb = new ProcessBuilder("osascript", "-e", script);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("AppleScript notification sent successfully");
                return true;
            } else {
                System.out.println("AppleScript notification failed with exit code: " + exitCode);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("AppleScript notification failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean showSystemTrayNotification(String message) {
        try {
            if (!SystemTray.isSupported()) {
                System.out.println("SystemTray not supported");
                return false;
            }
            
            System.out.println("Trying SystemTray notification...");
            
            SystemTray tray = SystemTray.getSystemTray();
            Image image = createNotificationIcon();
            
            TrayIcon trayIcon = new TrayIcon(image, "Habit Tracker");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Habit Tracker Reminder");
            
            tray.add(trayIcon);
            
            trayIcon.displayMessage("Habit Tracker Reminder", message, 
                                   TrayIcon.MessageType.INFO);
            
            // Remove tray icon after 5 seconds
            Timer removeTimer = new Timer();
            removeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    tray.remove(trayIcon);
                    removeTimer.cancel();
                }
            }, 5000);
            
            return true;
            
        } catch (Exception e) {
            System.out.println("SystemTray notification failed: " + e.getMessage());
            return false;
        }
    }
    
    private Image createNotificationIcon() {
        // Create a simple 16x16 green circle icon
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.GREEN);
        g2d.fillOval(2, 2, 12, 12);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawOval(2, 2, 12, 12);
        g2d.dispose();
        return image;
    }
    
    public void testNotification() {
        System.out.println("Sending test notification...");
        showNotification();
    }
}