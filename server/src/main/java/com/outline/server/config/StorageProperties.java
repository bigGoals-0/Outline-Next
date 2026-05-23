package com.outline.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outline.storage")
public record StorageProperties(String uploadDir) {}
