package com.outline.server.service;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ErrorReportingService {
    private static final Logger log = LoggerFactory.getLogger(ErrorReportingService.class);

    public Map<String, String> report(String source, String detail) {
        log.warn("Client error report from {}: {}", source, detail);
        return Map.of("status", "received", "timestamp", Instant.now().toString());
    }
}
