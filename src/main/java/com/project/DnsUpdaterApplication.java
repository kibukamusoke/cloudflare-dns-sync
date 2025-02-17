package com.project;

import com.project.cloudflare.CloudflareClient;
import com.project.config.DnsUpdaterConfig;
import com.project.core.IpMonitoringService;
import com.project.ip.IpAddressProvider;
import com.project.ip.IpifyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DnsUpdaterApplication {
    private static final Logger logger = LoggerFactory.getLogger(DnsUpdaterApplication.class);
    private static final String DEFAULT_CONFIG_PATH = "config/config.yml";

    private final DnsUpdaterConfig config;
    private final IpMonitoringService monitoringService;

    public DnsUpdaterApplication(String configPath) throws Exception {
        // Load configuration
        config = DnsUpdaterConfig.loadFromFile(configPath);
        
        // Create Cloudflare client
        CloudflareClient cloudflareClient = new CloudflareClient(
            config.getCloudflare().getApiToken(),
            config.getCloudflare().getZoneId()
        );

        // Create IP providers
        List<IpAddressProvider> ipProviders = List.of(
            new IpifyProvider()
            // Add more providers here as needed
        );

        // Create monitoring service
        monitoringService = new IpMonitoringService(ipProviders, cloudflareClient, config);
    }

    public void start() {
        logger.info("Starting DNS Updater");
        monitoringService.start();
    }

    public void stop() {
        logger.info("Stopping DNS Updater");
        monitoringService.stop();
    }

    public static void main(String[] args) {
        try {
            String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;
            DnsUpdaterApplication app = new DnsUpdaterApplication(configPath);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
            
            // Start the application
            app.start();
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }
} 