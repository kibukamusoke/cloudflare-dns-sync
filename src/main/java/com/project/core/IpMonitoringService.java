package com.project.core;

import com.project.cloudflare.CloudflareClient;
import com.project.cloudflare.CloudflareException;
import com.project.cloudflare.DnsRecord;
import com.project.config.DnsUpdaterConfig;
import com.project.ip.IpAddressProvider;
import com.project.ip.IpLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IpMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(IpMonitoringService.class);

    private final List<IpAddressProvider> ipProviders;
    private final CloudflareClient cloudflareClient;
    private final DnsUpdaterConfig config;
    private final ScheduledExecutorService executor;
    
    private InetAddress lastKnownIp;

    public IpMonitoringService(List<IpAddressProvider> ipProviders, 
                             CloudflareClient cloudflareClient,
                             DnsUpdaterConfig config) {
        this.ipProviders = ipProviders;
        this.cloudflareClient = cloudflareClient;
        this.config = config;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
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
            InetAddress currentIp = getCurrentIpAddress();
            if (currentIp == null) {
                logger.error("Failed to get current IP address from any provider");
                return;
            }

            if (lastKnownIp != null && currentIp.equals(lastKnownIp)) {
                logger.debug("IP address unchanged: {}", currentIp.getHostAddress());
                return;
            }

            updateDnsRecord(currentIp);
            lastKnownIp = currentIp;
        } catch (Exception e) {
            logger.error("Error during IP check", e);
        }
    }

    private InetAddress getCurrentIpAddress() {
        for (IpAddressProvider provider : ipProviders) {
            try {
                InetAddress ip = provider.getCurrentIpAddress();
                logger.debug("Got IP {} from provider {}", ip.getHostAddress(), provider.getProviderName());
                return ip;
            } catch (IpLookupException e) {
                logger.warn("Failed to get IP from provider {}: {}", 
                    provider.getProviderName(), e.getMessage());
            }
        }
        return null;
    }

    private void updateDnsRecord(InetAddress newIp) throws CloudflareException {
        DnsRecord record = cloudflareClient.getDnsRecord(
            config.getCloudflare().getRecordName(),
            config.getCloudflare().getRecordType()
        );

        if (record == null) {
            throw new CloudflareException("DNS record not found: " + 
                config.getCloudflare().getRecordName());
        }

        cloudflareClient.updateDnsRecord(
            record.getId(),
            record.getName(),
            record.getType(),
            newIp
        );
    }
} 