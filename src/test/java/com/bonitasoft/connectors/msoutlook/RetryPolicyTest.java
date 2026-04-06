package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void shouldReturnResultOnFirstAttempt() throws MsOutlookException {
        var policy = new RetryPolicy(3);
        String result = policy.execute(() -> "success");
        assertThat(result).isEqualTo("success");
    }

    @Test
    void shouldRetryOnRetryableException() throws MsOutlookException {
        var policy = new RetryPolicy(3) {
            @Override
            void sleep(long millis) {
                // Skip sleep in tests
            }
        };
        var counter = new int[]{0};
        String result = policy.execute(() -> {
            counter[0]++;
            if (counter[0] < 2) {
                throw new MsOutlookException("Rate limited", 429, true);
            }
            return "success";
        });
        assertThat(result).isEqualTo("success");
        assertThat(counter[0]).isEqualTo(2);
    }

    @Test
    void shouldFailImmediatelyOnNonRetryable() {
        var policy = new RetryPolicy(3);
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new MsOutlookException("Auth failed", 401, false);
        })).isInstanceOf(MsOutlookException.class)
           .hasMessageContaining("Auth failed");
    }

    @Test
    void shouldFailAfterMaxRetries() {
        var policy = new RetryPolicy(2) {
            @Override
            void sleep(long millis) {
                // Skip sleep
            }
        };
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new MsOutlookException("Server error", 500, true);
        })).isInstanceOf(MsOutlookException.class)
           .hasMessageContaining("Server error");
    }

    @Test
    void shouldWrapUnexpectedException() {
        var policy = new RetryPolicy(3);
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new RuntimeException("unexpected");
        })).isInstanceOf(MsOutlookException.class)
           .hasMessageContaining("Unexpected error");
    }

    @Test
    void shouldCalculateExponentialWait() {
        var policy = new RetryPolicy(3);
        long wait0 = policy.calculateWait(0);
        long wait1 = policy.calculateWait(1);
        long wait2 = policy.calculateWait(2);
        // Wait should increase (with jitter, so we check base)
        assertThat(wait0).isGreaterThanOrEqualTo(1000L);
        assertThat(wait1).isGreaterThanOrEqualTo(2000L);
        assertThat(wait2).isGreaterThanOrEqualTo(4000L);
    }

    @Test
    void shouldIdentifyRetryableStatusCodes() {
        assertThat(RetryPolicy.isRetryableStatusCode(429)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(500)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(502)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(503)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(504)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(400)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(401)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(403)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(404)).isFalse();
    }
}
