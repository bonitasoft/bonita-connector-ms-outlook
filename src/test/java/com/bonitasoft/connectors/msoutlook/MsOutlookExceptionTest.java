package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MsOutlookExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        var ex = new MsOutlookException("test error");
        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getStatusCode()).isEqualTo(-1);
        assertThat(ex.isRetryable()).isFalse();
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        var cause = new RuntimeException("root cause");
        var ex = new MsOutlookException("test error", cause);
        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateWithStatusCode() {
        var ex = new MsOutlookException("rate limited", 429, true);
        assertThat(ex.getStatusCode()).isEqualTo(429);
        assertThat(ex.isRetryable()).isTrue();
    }

    @Test
    void shouldCreateWithAllParameters() {
        var cause = new RuntimeException("cause");
        var ex = new MsOutlookException("error", 500, true, cause);
        assertThat(ex.getStatusCode()).isEqualTo(500);
        assertThat(ex.isRetryable()).isTrue();
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
