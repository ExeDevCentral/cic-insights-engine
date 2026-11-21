package com.exedev.cic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cic")
public record CicProperties(
        String canonicalBaseUrl,
        int maxPageSize,
        int defaultSuggestLimit
) {
    public CicProperties {
        canonicalBaseUrl = (canonicalBaseUrl == null || canonicalBaseUrl.isBlank())
                ? "https://insights.exedev.com/companies/"
                : canonicalBaseUrl.endsWith("/") ? canonicalBaseUrl : canonicalBaseUrl + "/";
        maxPageSize = maxPageSize > 0 ? Math.min(maxPageSize, 100) : 50;
        defaultSuggestLimit = defaultSuggestLimit > 0 ? Math.min(defaultSuggestLimit, 20) : 8;
    }
}
