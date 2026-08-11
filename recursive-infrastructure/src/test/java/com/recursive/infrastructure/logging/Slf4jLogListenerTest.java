package com.recursive.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.recursive.application.LogService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Slf4jLogListenerTest {

    @Test
    void forwardsEntriesToSlf4jAtMatchingLevels() {
        List<ILoggingEvent> events = new ArrayList<>();
        AppenderBase<ILoggingEvent> capture = new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent event) {
                events.add(event);
            }
        };
        capture.start();
        Logger logger = (Logger) LoggerFactory.getLogger("Recursive");
        logger.addAppender(capture);
        try {
            LogService logService = new LogService();
            Slf4jLogListener.attachTo(logService);

            logService.log(LogService.Level.WARN, "low disk space", "monitor");

            assertThat(events).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getMessage()).isEqualTo("low disk space");
            });
        } finally {
            logger.detachAppender(capture);
        }
    }
}
