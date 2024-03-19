package org.example;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public class Options {

    private Duration interval = Duration.ofMillis(10);

    private Optional<Path> flamePath;

    private boolean printMethodTable = true;

    public Duration getInterval() {
        return interval;
    }

    public void setInterval(Duration interval) {
        this.interval = interval;
    }

    public Optional<Path> getFlamePath() {
        return flamePath;
    }

    public void setFlamePath(Optional<Path> flamePath) {
        this.flamePath = flamePath;
    }

    public boolean isPrintMethodTable() {
        return printMethodTable;
    }

    public void setPrintMethodTable(boolean printMethodTable) {
        this.printMethodTable = printMethodTable;
    }
}
