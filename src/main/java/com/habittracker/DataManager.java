package com.habittracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataManager {
    private static final String DATA_FILE = "habits.json";
    private static final String BACKUP_FILE = "habits_backup.json";
    private final ObjectMapper objectMapper;
    private final Path dataPath;
    private final Path backupPath;
    
    public DataManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, ".habit-tracker");
        this.dataPath = appDir.resolve(DATA_FILE);
        this.backupPath = appDir.resolve(BACKUP_FILE);
        
        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create application directory", e);
        }
    }
    
    public HabitData loadData() {
        try {
            if (Files.exists(dataPath)) {
                return objectMapper.readValue(dataPath.toFile(), HabitData.class);
            }
        } catch (IOException e) {
            System.err.println("Error loading data, trying backup: " + e.getMessage());
            try {
                if (Files.exists(backupPath)) {
                    return objectMapper.readValue(backupPath.toFile(), HabitData.class);
                }
            } catch (IOException backupError) {
                System.err.println("Error loading backup: " + backupError.getMessage());
            }
        }
        return new HabitData();
    }
    
    public void saveData(HabitData data) {
        try {
            if (Files.exists(dataPath)) {
                Files.copy(dataPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            objectMapper.writeValue(dataPath.toFile(), data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save data", e);
        }
    }
    
    public String getDataFilePath() {
        return dataPath.toString();
    }
}