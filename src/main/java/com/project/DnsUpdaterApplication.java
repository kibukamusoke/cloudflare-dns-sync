package com.project;

import com.project.cloudflare.CloudflareClient;
import com.project.config.DnsUpdaterConfig;
import com.project.core.IpMonitoringService;
import com.project.ip.IpAddressProvider;
import com.project.ip.IpifyProvider;
import com.project.ip.IpApiProvider;
import com.project.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DnsUpdaterApplication {
    private static final Logger logger = LoggerFactory.getLogger(DnsUpdaterApplication.class);
    private static final String DEFAULT_CONFIG_PATH = "config/config.yml";
    private static final String VERSION = "1.0.0";

    private volatile boolean isRunning = true;

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
            new IpifyProvider(),
            new IpApiProvider()
        );

        // Create notification service
        NotificationService notificationService = new NotificationService(config);

        // Create monitoring service
        monitoringService = new IpMonitoringService(ipProviders, cloudflareClient, config, notificationService);
    }

    public void start() {
        logger.info("Starting CloudDNSync v{}", VERSION);
        monitoringService.start();
    }

    public void stop() {
        logger.info("Gracefully shutting down CloudDNSync");
        isRunning = false;
        monitoringService.stop();
        logger.info("Shutdown complete");
    }

    public static void main(String[] args) {
        // Check for version flag
        if (args.length > 0 && args[0].equals("--version")) {
            System.out.println("CloudDNSync version " + VERSION);
            System.exit(0);
        }

        // Check for debug flag
        if (args.length > 0 && args[0].equals("--debug")) {
            System.setProperty("logging.level.root", "DEBUG");
        }

        try {
            String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;
            // Skip flags when looking for config path
            if (configPath.startsWith("--")) {
                configPath = DEFAULT_CONFIG_PATH;
            }
            DnsUpdaterApplication app = new DnsUpdaterApplication(configPath);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    app.stop();
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));
            
            app.start();

            // Keep main thread alive
            while (app.isRunning) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }
} 