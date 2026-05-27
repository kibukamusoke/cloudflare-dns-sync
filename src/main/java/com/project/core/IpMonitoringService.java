package com.project.core;

import com.project.cloudflare.CloudflareClient;
import com.project.cloudflare.CloudflareException;
import com.project.cloudflare.DnsRecord;
import com.project.config.DnsUpdaterConfig;
import com.project.config.DnsUpdaterConfig.RecordConfig;
import com.project.ip.IpAddressProvider;
import com.project.ip.IpLookupException;
import com.project.ip.IpVersion;
import com.project.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IpMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(IpMonitoringService.class);

    private final List<IpAddressProvider> ipProviders;
    private final CloudflareClient cloudflareClient;
    private final DnsUpdaterConfig config;
    private final NotificationService notificationService;
    private final ScheduledExecutorService executor;
    private final List<RecordTarget> targets;

    public IpMonitoringService(List<IpAddressProvider> ipProviders,
                               CloudflareClient cloudflareClient,
                               DnsUpdaterConfig config,
                               NotificationService notificationService) {
        this.ipProviders = ipProviders;
        this.cloudflareClient = cloudflareClient;
        this.config = config;
        this.notificationService = notificationService;
        this.executor = Executors.newSingleThreadScheduledExecutor();

        this.targets = new ArrayList<>();
        for (RecordConfig r : config.getCloudflare().getResolvedRecords()) {
            targets.add(new RecordTarget(r));
        }
    }

    public void start() {
        logger.info("Monitoring {} DNS record(s) every {}s", targets.size(),
            config.getMonitoring().getCheckInterval());
        executor.scheduleWithFixedDelay(
            this::checkIpAddress,
            0,
            config.getMonitoring().getCheckInterval(),
            TimeUnit.SECONDS
        );
    }

    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void checkIpAddress() {
        try {
            // Resolve the current address once per IP version that is actually needed.
            Set<IpVersion> needed = EnumSet.noneOf(IpVersion.class);
            for (RecordTarget t : targets) {
                needed.add(t.version);
            }

            Map<IpVersion, String> currentIps = new EnumMap<>(IpVersion.class);
            for (IpVersion version : needed) {
                String ip = resolveCurrentIp(version);
                if (ip != null) {
                    currentIps.put(version, ip);
                } else {
                    logger.error("Failed to determine current {} address from any provider", version);
                }
            }

            for (RecordTarget target : targets) {
                String ip = currentIps.get(target.version);
                if (ip != null) {
                    reconcile(target, ip);
                }
            }
        } catch (Exception e) {
            logger.error("Error during IP check", e);
        }
    }

    /**
     * Tries every provider in order, retrying the whole chain up to
     * {@code maxRetries} times with {@code retryInterval} seconds between
     * attempts (the behaviour documented in the README).
     */
    private String resolveCurrentIp(IpVersion version) {
        int maxRetries = Math.max(1, config.getMonitoring().getMaxRetries());
        int retryInterval = config.getMonitoring().getRetryInterval();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            for (IpAddressProvider provider : ipProviders) {
                try {
                    String ip = provider.getCurrentIpAddress(version);
                    logger.debug("Got {} address {} from provider {}", version, ip, provider.getProviderName());
                    return ip;
                } catch (IpLookupException e) {
                    logger.warn("Failed to get {} address from provider {}: {}",
                        version, provider.getProviderName(), e.getMessage());
                }
            }

            if (attempt < maxRetries) {
                logger.warn("All providers failed for {} (attempt {}/{}); retrying in {}s",
                    version, attempt, maxRetries, retryInterval);
                if (!sleep(retryInterval)) {
                    return null; // interrupted during shutdown
                }
            }
        }
        return null;
    }

    /**
     * Ensures the target record matches {@code newIp}. The live Cloudflare
     * record content is the source of truth, so a restart with an unchanged IP
     * causes no update and no notification.
     */
    private void reconcile(RecordTarget target, String newIp) {
        if (newIp.equals(target.lastKnownIp)) {
            logger.debug("{} unchanged: {}", target.recordName, newIp);
            return;
        }

        try {
            DnsRecord record = cloudflareClient.getDnsRecord(
                target.apiToken, target.zoneId, target.recordName, target.recordType);

            if (record == null) {
                logger.info("DNS record {} ({}) does not exist; creating it pointing to {}",
                    target.recordName, target.recordType, newIp);
                cloudflareClient.createDnsRecord(
                    target.apiToken, target.zoneId, target.recordName, target.recordType, newIp);
                target.lastKnownIp = newIp;
                notificationService.notifyIpChange(target.recordName, newIp);
                return;
            }

            if (newIp.equals(record.getContent())) {
                logger.debug("{} already set to {} in Cloudflare; no update needed",
                    target.recordName, newIp);
                target.lastKnownIp = newIp;
                return;
            }

            cloudflareClient.updateDnsRecord(
                target.apiToken, target.zoneId, record.getId(),
                record.getName(), record.getType(), newIp);
            target.lastKnownIp = newIp;
            notificationService.notifyIpChange(target.recordName, newIp);
        } catch (CloudflareException e) {
            logger.error("Failed to update {}: {}", target.recordName, e.getMessage());
        }
    }

    private boolean sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** One DNS record to keep in sync, with its resolved credentials and last-seen IP. */
    private static final class RecordTarget {
        final String apiToken;
        final String zoneId;
        final String recordName;
        final String recordType;
        final IpVersion version;
        volatile String lastKnownIp;

        RecordTarget(RecordConfig config) {
            this.apiToken = config.getApiToken();
            this.zoneId = config.getZoneId();
            this.recordName = config.getRecordName();
            this.recordType = config.getRecordType();
            this.version = IpVersion.fromRecordType(config.getRecordType());
        }
    }
}
