package com.project;

import com.project.cloudflare.CloudflareClient;
import com.project.config.DnsUpdaterConfig;
import com.project.core.IpMonitoringService;
import com.project.ip.IcanhazipProvider;
import com.project.ip.IpAddressProvider;
import com.project.ip.IpifyProvider;
import com.project.notifications.NotificationService;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DnsUpdaterApplication {
    // NOTE: no static logger here on purpose. The logger is only obtained after
    // applyLoggingConfig() has set the logback system properties, otherwise
    // logging would initialise with default settings before config is read.
    private static final String DEFAULT_CONFIG_PATH = "config/config.yml";
    private static final String VERSION = "1.0.0";

    private volatile boolean isRunning = true;

    private final Logger logger = LoggerFactory.getLogger(DnsUpdaterApplication.class);
    private final CloseableHttpClient httpClient;
    private final IpMonitoringService monitoringService;

    public DnsUpdaterApplication(DnsUpdaterConfig config) {
        // One HTTP client shared across IP lookups, Cloudflare calls and notifications.
        this.httpClient = HttpClients.createDefault();

        CloudflareClient cloudflareClient = new CloudflareClient(httpClient);

        List<IpAddressProvider> ipProviders = List.of(
            new IpifyProvider(httpClient),
            new IcanhazipProvider(httpClient)
        );

        NotificationService notificationService = new NotificationService(config, httpClient);

        this.monitoringService = new IpMonitoringService(
            ipProviders, cloudflareClient, config, notificationService);
    }

    public void start() {
        logger.info("Starting CloudDNSync v{}", VERSION);
        monitoringService.start();
    }

    public void stop() {
        logger.info("Gracefully shutting down CloudDNSync");
        isRunning = false;
        monitoringService.stop();
        try {
            httpClient.close();
        } catch (Exception e) {
            logger.warn("Error closing HTTP client", e);
        }
        logger.info("Shutdown complete");
    }

    public static void main(String[] args) {
        // Resolve flags and config path before any logging is initialised.
        String configPath = System.getenv().getOrDefault("CONFIG_PATH", DEFAULT_CONFIG_PATH);
        boolean debug = false;
        for (String arg : args) {
            if (arg.equals("--version")) {
                System.out.println("CloudDNSync version " + VERSION);
                return;
            } else if (arg.equals("--debug")) {
                debug = true;
            } else if (!arg.startsWith("--")) {
                configPath = arg;
            }
        }

        DnsUpdaterConfig config;
        try {
            config = DnsUpdaterConfig.loadFromFile(configPath);
            config.validate();
        } catch (Exception e) {
            // Logging isn't configured yet; print to stderr and bail out.
            System.err.println("Failed to load configuration from " + configPath + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        applyLoggingConfig(config, debug);
        Logger logger = LoggerFactory.getLogger(DnsUpdaterApplication.class);

        try {
            DnsUpdaterApplication app = new DnsUpdaterApplication(config);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    app.stop();
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));

            app.start();

            while (app.isRunning) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    /**
     * Feeds the logging config into logback via system properties. Must run
     * before the first logger is created (see field note above).
     */
    private static void applyLoggingConfig(DnsUpdaterConfig config, boolean debug) {
        DnsUpdaterConfig.LoggingConfig log = config.getLogging();
        String level = debug ? "DEBUG" : log.getLevel();
        setIfPresent("LOG_LEVEL", level);
        setIfPresent("LOG_FILE", log.getFile());
        setIfPresent("LOG_MAX_SIZE", log.getMaxSize());
        if (log.getMaxBackups() > 0) {
            System.setProperty("LOG_MAX_HISTORY", String.valueOf(log.getMaxBackups()));
        }
    }

    private static void setIfPresent(String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            System.setProperty(key, value);
        }
    }
}
