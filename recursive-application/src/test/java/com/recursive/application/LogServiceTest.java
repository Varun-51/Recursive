package com.recursive.application;

import com.recursive.application.LogService.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogServiceTest {

    @Test
    void returnsRecentEntriesOldestFirst() {
        LogService service = new LogService();
        service.log(Level.INFO, "first", "src");
        service.log(Level.ERROR, "second", "src");

        List<LogService.LogEntry> entries = service.recent(10);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).message()).isEqualTo("first");
        assertThat(entries.get(1).message()).isEqualTo("second");
    }

    @Test
    void ringBufferNeverGrowsPastCapacity() {
        LogService service = new LogService();
        for (int i = 0; i < 1000; i++) {
            service.log(Level.DEBUG, "m" + i, "src");
        }

        assertThat(service.recent(10_000)).hasSize(500);
    }

    @Test
    void listenersReceiveEveryEntry() {
        LogService service = new LogService();
        AtomicInteger received = new AtomicInteger();
        service.addListener(entry -> received.incrementAndGet());
        service.log(Level.WARN, "watch out", "src");

        assertThat(received.get()).isEqualTo(1);
    }

    @Test
    void rejectsBlankMessage() {
        LogService service = new LogService();
        assertThatThrownBy(() -> service.log(Level.INFO, "  ", "src"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
