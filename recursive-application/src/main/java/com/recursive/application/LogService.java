package com.recursive.application;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Central in-process log collector. Entries live in a bounded ring so the
 * log viewer never grows unbounded; listeners (UI panels, SLF4J bridge)
 * receive every entry. Thread-safe: workers log from any thread.
 */
public class LogService {

    public record LogEntry(Level level, String message, String source, Instant timestamp) {
    }

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    private static final int CAPACITY = 500;

    private final LogEntry[] buffer = new LogEntry[CAPACITY];
    private final AtomicInteger cursor = new AtomicInteger();
    private final List<Consumer<LogEntry>> listeners = new CopyOnWriteArrayList<>();

    public void log(Level level, String message, String source) {
        if (level == null || message == null || message.isBlank() || source == null) {
            throw new IllegalArgumentException("level, message, and source are required");
        }
        LogEntry entry = new LogEntry(level, message, source, Instant.now());
        buffer[cursor.getAndUpdate(i -> (i + 1) % CAPACITY)] = entry;
        listeners.forEach(listener -> listener.accept(entry));
    }

    /**
     * @return the most recent entries, oldest first, at most {@code max}
     */
    public List<LogEntry> recent(int max) {
        int count = Math.min(max, CAPACITY);
        List<LogEntry> entries = new java.util.ArrayList<>(count);
        int current = cursor.get();
        for (int i = count - 1; i >= 0; i--) {
            LogEntry entry = buffer[Math.floorMod(current - i - 1, CAPACITY)];
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public void addListener(Consumer<LogEntry> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<LogEntry> listener) {
        listeners.remove(listener);
    }
}
