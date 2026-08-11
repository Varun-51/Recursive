package com.recursive.infrastructure.logging;

import com.recursive.application.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Bridges the application's {@link LogService} into SLF4J so infrastructure
 * logging and application log entries land in the same output.
 */
public class Slf4jLogListener implements Consumer<LogService.LogEntry> {

    private static final Logger log = LoggerFactory.getLogger("Recursive");

    @Override
    public void accept(LogService.LogEntry entry) {
        switch (entry.level()) {
            case DEBUG -> log.debug(entry.message());
            case INFO -> log.info(entry.message());
            case WARN -> log.warn(entry.message());
            case ERROR -> log.error(entry.message());
        }
    }

    public static void attachTo(LogService logService) {
        logService.addListener(new Slf4jLogListener());
    }
}
