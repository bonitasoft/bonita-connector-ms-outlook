package com.bonitasoft.connectors.msoutlook;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

class TemplateRenderingTest {

    @Test
    void shouldEscapeHtml() {
        String result = MsOutlookClient.escapeHtml("<script>alert('xss')</script>");
        assertThat(result).isEqualTo("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
    }

    @Test
    void shouldEscapeAmpersand() {
        assertThat(MsOutlookClient.escapeHtml("A & B")).isEqualTo("A &amp; B");
    }

    @Test
    void shouldEscapeQuotes() {
        assertThat(MsOutlookClient.escapeHtml("say \"hello\"")).isEqualTo("say &quot;hello&quot;");
    }

    @Test
    void shouldReturnEmptyForNull() {
        assertThat(MsOutlookClient.escapeHtml(null)).isEqualTo("");
    }
}
