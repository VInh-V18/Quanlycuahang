package com.quanlycuahang.erp.platform.dto;

import java.time.Instant;

public record TenantResponse(Long id, String name, boolean active, Instant createdAt) {}
