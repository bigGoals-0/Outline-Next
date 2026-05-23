package com.outline.server.controller;

import com.outline.server.service.ErrorReportingService;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
@ConditionalOnProperty(prefix = "outline.debug", name = "enabled", havingValue = "true")
public class DebugController {
    private final ErrorReportingService errorReportingService;

    public DebugController(ErrorReportingService errorReportingService) {
        this.errorReportingService = errorReportingService;
    }

    @PostMapping("/report")
    Map<String, String> report(@RequestBody Map<String, String> body) {
        return errorReportingService.report(body.getOrDefault("source", "client"), body.getOrDefault("detail", ""));
    }
}
