package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MsOutlookConfigurationTest {

    @Test
    void shouldBuildWithDefaults() {
        var config = MsOutlookConfiguration.builder()
                .tenantId("tenant")
                .clientId("client")
                .clientSecret("secret")
                .senderUserId("user@test.com")
                .build();

        assertThat(config.getTenantId()).isEqualTo("tenant");
        assertThat(config.getClientId()).isEqualTo("client");
        assertThat(config.getClientSecret()).isEqualTo("secret");
        assertThat(config.getSenderUserId()).isEqualTo("user@test.com");
        assertThat(config.getBasePath()).isEqualTo("https://graph.microsoft.com/v1.0");
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
        assertThat(config.getMaxRetries()).isEqualTo(3);
    }

    @Test
    void shouldBuildWithCustomValues() {
        var config = MsOutlookConfiguration.builder()
                .tenantId("t1")
                .clientId("c1")
                .clientSecret("s1")
                .senderUserId("u1")
                .basePath("https://graph.microsoft.com/beta")
                .connectTimeout(5000)
                .readTimeout(10000)
                .maxRetries(5)
                .build();

        assertThat(config.getBasePath()).isEqualTo("https://graph.microsoft.com/beta");
        assertThat(config.getConnectTimeout()).isEqualTo(5000);
        assertThat(config.getReadTimeout()).isEqualTo(10000);
        assertThat(config.getMaxRetries()).isEqualTo(5);
    }
}
