package com.project.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.project.config.DnsUpdaterConfig.RecordConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DnsUpdaterConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private DnsUpdaterConfig parse(String yaml) throws Exception {
        return MAPPER.readValue(yaml, DnsUpdaterConfig.class);
    }

    @Test
    void resolvesMultipleRecordsAcrossZonesWithDefaults() throws Exception {
        DnsUpdaterConfig config = parse(String.join("\n",
            "cloudflare:",
            "  apiToken: account-token",
            "  records:",
            "    - zoneId: zone-a",
            "      recordName: home.example.com",
            "      recordType: A",
            "    - zoneId: zone-b",
            "      recordName: vpn.example.org",
            "      recordType: AAAA",
            "      apiToken: per-record-token",
            "    - zoneId: zone-c",
            "      recordName: bare.example.net"));

        List<RecordConfig> records = config.getCloudflare().getResolvedRecords();
        assertEquals(3, records.size());

        // Inherits the account token; keeps its explicit type
        assertEquals("account-token", records.get(0).getApiToken());
        assertEquals("A", records.get(0).getRecordType());

        // Per-record token override is honoured
        assertEquals("per-record-token", records.get(1).getApiToken());
        assertEquals("AAAA", records.get(1).getRecordType());

        // recordType defaults to A when omitted
        assertEquals("account-token", records.get(2).getApiToken());
        assertEquals("A", records.get(2).getRecordType());
    }

    @Test
    void acceptsLegacySingleRecordFormat() throws Exception {
        DnsUpdaterConfig config = parse(String.join("\n",
            "cloudflare:",
            "  apiToken: token",
            "  zoneId: zone-legacy",
            "  recordName: legacy.example.com",
            "  recordType: A"));

        List<RecordConfig> records = config.getCloudflare().getResolvedRecords();
        assertEquals(1, records.size());
        assertEquals("zone-legacy", records.get(0).getZoneId());
        assertEquals("legacy.example.com", records.get(0).getRecordName());
        assertEquals("token", records.get(0).getApiToken());
    }

    @Test
    void validateRejectsMissingToken() throws Exception {
        DnsUpdaterConfig config = parse(String.join("\n",
            "cloudflare:",
            "  records:",
            "    - zoneId: zone-a",
            "      recordName: home.example.com"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, config::validate);
        assertTrue(ex.getMessage().contains("API token"));
    }

    @Test
    void validateRejectsNoRecords() throws Exception {
        DnsUpdaterConfig config = parse(String.join("\n",
            "cloudflare:",
            "  apiToken: token"));

        assertThrows(IllegalStateException.class, config::validate);
    }
}
