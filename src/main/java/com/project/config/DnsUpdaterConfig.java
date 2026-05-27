package com.project.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DnsUpdaterConfig {
    private CloudflareConfig cloudflare = new CloudflareConfig();
    private MonitoringConfig monitoring = new MonitoringConfig();
    private LoggingConfig logging = new LoggingConfig();
    private NotificationsConfig notifications = new NotificationsConfig();

    public static DnsUpdaterConfig loadFromFile(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(path), DnsUpdaterConfig.class);
    }

    /**
     * Validates the loaded configuration, throwing a descriptive error when
     * required values are missing. Call this once after loading.
     */
    public void validate() {
        List<RecordConfig> records = cloudflare.getResolvedRecords();
        if (records.isEmpty()) {
            throw new IllegalStateException(
                "No DNS records configured. Add at least one entry under cloudflare.records.");
        }
        for (int i = 0; i < records.size(); i++) {
            RecordConfig r = records.get(i);
            String where = "cloudflare.records[" + i + "]";
            if (isBlank(r.getApiToken())) {
                throw new IllegalStateException("Missing API token for " + where
                    + " (set cloudflare.apiToken or a per-record apiToken).");
            }
            if (isBlank(r.getZoneId())) {
                throw new IllegalStateException("Missing zoneId for " + where + ".");
            }
            if (isBlank(r.getRecordName())) {
                throw new IllegalStateException("Missing recordName for " + where + ".");
            }
        }
        if (monitoring.getCheckInterval() <= 0) {
            throw new IllegalStateException("monitoring.checkInterval must be greater than 0.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // Getters and setters
    public CloudflareConfig getCloudflare() {
        return cloudflare;
    }

    public void setCloudflare(CloudflareConfig cloudflare) {
        this.cloudflare = cloudflare;
    }

    public MonitoringConfig getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringConfig monitoring) {
        this.monitoring = monitoring;
    }

    public LoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(LoggingConfig logging) {
        this.logging = logging;
    }

    public NotificationsConfig getNotifications() {
        return notifications;
    }

    public void setNotifications(NotificationsConfig notifications) {
        this.notifications = notifications;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CloudflareConfig {
        /** Account-wide token used for every record unless a record overrides it. */
        private String apiToken;
        private List<RecordConfig> records = new ArrayList<>();

        // Legacy single-record fields (config format < 1.1). Still accepted so
        // existing installs keep working after an upgrade.
        private String zoneId;
        private String recordName;
        private String recordType;

        /**
         * Returns the records to manage with defaults applied (effective token
         * and record type). Falls back to the legacy single-record fields when
         * no {@code records} list is present.
         */
        public List<RecordConfig> getResolvedRecords() {
            List<RecordConfig> source = records;
            if ((source == null || source.isEmpty()) && recordName != null) {
                RecordConfig legacy = new RecordConfig();
                legacy.setZoneId(zoneId);
                legacy.setRecordName(recordName);
                legacy.setRecordType(recordType);
                source = List.of(legacy);
            }
            if (source == null) {
                return List.of();
            }

            List<RecordConfig> resolved = new ArrayList<>(source.size());
            for (RecordConfig r : source) {
                RecordConfig copy = new RecordConfig();
                copy.setApiToken(r.getApiToken() != null ? r.getApiToken() : apiToken);
                copy.setZoneId(r.getZoneId());
                copy.setRecordName(r.getRecordName());
                copy.setRecordType(r.getRecordType() != null ? r.getRecordType() : "A");
                resolved.add(copy);
            }
            return resolved;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public List<RecordConfig> getRecords() {
            return records;
        }

        public void setRecords(List<RecordConfig> records) {
            this.records = records;
        }

        public String getZoneId() {
            return zoneId;
        }

        public void setZoneId(String zoneId) {
            this.zoneId = zoneId;
        }

        public String getRecordName() {
            return recordName;
        }

        public void setRecordName(String recordName) {
            this.recordName = recordName;
        }

        public String getRecordType() {
            return recordType;
        }

        public void setRecordType(String recordType) {
            this.recordType = recordType;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecordConfig {
        /** Optional per-record token overriding {@link CloudflareConfig#apiToken}. */
        private String apiToken;
        private String zoneId;
        private String recordName;
        private String recordType;

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getZoneId() {
            return zoneId;
        }

        public void setZoneId(String zoneId) {
            this.zoneId = zoneId;
        }

        public String getRecordName() {
            return recordName;
        }

        public void setRecordName(String recordName) {
            this.recordName = recordName;
        }

        public String getRecordType() {
            return recordType;
        }

        public void setRecordType(String recordType) {
            this.recordType = recordType;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MonitoringConfig {
        private int checkInterval = 300;
        private int retryInterval = 60;
        private int maxRetries = 3;

        public int getCheckInterval() {
            return checkInterval;
        }

        public void setCheckInterval(int checkInterval) {
            this.checkInterval = checkInterval;
        }

        public int getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoggingConfig {
        private String level = "INFO";
        private String file = "logs/clouddnsync.log";
        private String maxSize = "10MB";
        private int maxBackups = 5;

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(String maxSize) {
            this.maxSize = maxSize;
        }

        public int getMaxBackups() {
            return maxBackups;
        }

        public void setMaxBackups(int maxBackups) {
            this.maxBackups = maxBackups;
        }
    }
}
