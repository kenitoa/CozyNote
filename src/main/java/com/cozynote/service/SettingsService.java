package com.cozynote.service;

import java.io.IOException;

import com.cozynote.persistence.SqliteNoteRepository;

public final class SettingsService {
    private final SqliteNoteRepository repository;

    public SettingsService(SqliteNoteRepository repository) {
        this.repository = repository;
    }

    public boolean getBoolean(String key, boolean fallback) {
        try {
            return Boolean.parseBoolean(repository.getSetting(key, Boolean.toString(fallback)));
        } catch (IOException exception) {
            return fallback;
        }
    }

    public void setBoolean(String key, boolean value) {
        save(key, Boolean.toString(value));
    }

    public double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(repository.getSetting(key, Double.toString(fallback)));
        } catch (IOException | NumberFormatException exception) {
            return fallback;
        }
    }

    public void setDouble(String key, double value) {
        save(key, Double.toString(value));
    }

    public String getString(String key, String fallback) {
        try {
            return repository.getSetting(key, fallback);
        } catch (IOException exception) {
            return fallback;
        }
    }

    public void setString(String key, String value) {
        save(key, value == null ? "" : value);
    }

    private void save(String key, String value) {
        try {
            repository.saveSetting(key, value);
        } catch (IOException ignored) {
            // Settings changes should not interrupt note editing.
        }
    }
}
