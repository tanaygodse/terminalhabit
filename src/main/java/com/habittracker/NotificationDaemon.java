package com.habittracker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationDaemon {
    private static final String DAEMON_DIR = System.getProperty("user.home") + "/.habit-tracker";
    private static final String PID_FILE = DAEMON_DIR + "/daemon.pid";
    private static final String LOG_FILE = DAEMON_DIR + "/daemon.log";
    
    private final HabitService habitService;
    private final NotificationService notificationService;
    
    public NotificationDaemon() {
        this.habitService = new HabitService();
        this.notificationService = new NotificationService(habitService);
    }
    
    public static void main(String[] args) {
        if (args.length > 0 && "daemon-process".equals(args[0])) {
            // This is the actual daemon process
            NotificationDaemon daemon = new NotificationDaemon();
            daemon.runDaemon();
        } else {
            System.err.println("This is an internal daemon process. Use the CLI commands instead.");
            System.exit(1);
        }
    }
    
    /**
     * Start the daemon in the background
     */
    public boolean startDaemon() {
        try {
            // Check if daemon is already running
            if (isDaemonRunning()) {
                System.out.println("Daemon is already running");
                return false;
            }
            
            // Create daemon directory if it doesn't exist
            Path daemonDir = Paths.get(DAEMON_DIR);
            if (!Files.exists(daemonDir)) {
                Files.createDirectories(daemonDir);
            }
            
            // Get the current JAR path
            String jarPath = getJarPath();
            if (jarPath == null) {
                System.err.println("Could not determine JAR path");
                return false;
            }
            
            // Build command to start daemon process
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(jarPath);
            command.add("daemon-process");
            
            // Start the daemon process
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(new File(LOG_FILE));
            pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(LOG_FILE)));
            
            Process process = pb.start();
            
            // Write PID to file (Java 8 compatible approach)
            writePidFile(getProcessId(process));
            
            System.out.println("✓ Daemon started successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to start daemon: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Stop the daemon
     */
    public boolean stopDaemon() {
        try {
            Optional<Long> pid = readPidFile();
            if (!pid.isPresent()) {
                System.out.println("No daemon PID file found");
                return false;
            }
            
            // Try to kill the process
            ProcessBuilder pb = new ProcessBuilder("kill", String.valueOf(pid.get()));
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Clean up PID file
                Files.deleteIfExists(Paths.get(PID_FILE));
                System.out.println("✓ Daemon stopped successfully");
                return true;
            } else {
                System.err.println("Failed to stop daemon (exit code: " + exitCode + ")");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Failed to stop daemon: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if daemon is running
     */
    public boolean isDaemonRunning() {
        try {
            Optional<Long> pid = readPidFile();
            if (!pid.isPresent()) {
                return false;
            }
            
            // Check if process is still running
            ProcessBuilder pb = new ProcessBuilder("kill", "-0", String.valueOf(pid.get()));
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                // Process is not running, clean up stale PID file
                Files.deleteIfExists(Paths.get(PID_FILE));
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get daemon status
     */
    public void showDaemonStatus() {
        if (isDaemonRunning()) {
            Optional<Long> pid = readPidFile();
            System.out.println("✓ Daemon is running (PID: " + pid.orElse(0L) + ")");
            
            // Show alert settings
            Optional<AlertSettings> alertSettings = habitService.getAlertSettings();
            if (alertSettings.isPresent()) {
                AlertSettings settings = alertSettings.get();
                if (settings.isEnabled()) {
                    System.out.println("  Alert time: " + settings.getAlertTime());
                } else {
                    System.out.println("  Alerts are disabled");
                }
            } else {
                System.out.println("  No alert configured");
            }
        } else {
            System.out.println("✗ Daemon is not running");
        }
    }
    
    /**
     * Run the daemon process (called by background process)
     */
    private void runDaemon() {
        try {
            System.out.println("Starting notification daemon...");
            
            // Set up shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down daemon...");
                notificationService.stopNotifications();
                try {
                    Files.deleteIfExists(Paths.get(PID_FILE));
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }));
            
            // Start notifications
            notificationService.startNotifications(true);
            
            // Keep daemon alive
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
        } catch (Exception e) {
            System.err.println("Daemon error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the path to the current JAR file
     */
    private String getJarPath() {
        try {
            return NotificationDaemon.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get process ID in Java 8 compatible way
     */
    private long getProcessId(Process process) {
        try {
            // Use reflection to get PID from Process object
            java.lang.reflect.Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            return pidField.getLong(process);
        } catch (Exception e) {
            // Fallback: return 0 if we can't get PID
            System.err.println("Warning: Could not get process ID: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Write PID to file
     */
    private void writePidFile(long pid) throws IOException {
        try (FileWriter writer = new FileWriter(PID_FILE)) {
            writer.write(String.valueOf(pid));
        }
    }
    
    /**
     * Read PID from file
     */
    private Optional<Long> readPidFile() {
        try {
            if (!Files.exists(Paths.get(PID_FILE))) {
                return Optional.empty();
            }
            
            String pidStr = new String(Files.readAllBytes(Paths.get(PID_FILE))).trim();
            return Optional.of(Long.parseLong(pidStr));
            
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}