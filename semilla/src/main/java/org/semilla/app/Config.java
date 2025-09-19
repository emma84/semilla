package org.semilla.app;

public class Config {
    public String dbPath;
    public String backupDir;
    public String roundingRule;

    @Override public String toString() {
        return "Config{dbPath='%s', backupDir='%s', roundingRule='%s'}"
                .formatted(dbPath, backupDir, roundingRule);
    }
}
