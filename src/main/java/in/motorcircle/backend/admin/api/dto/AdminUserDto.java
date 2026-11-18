package in.motorcircle.backend.admin.api.dto;

import java.time.Instant;

public record AdminUserDto(
    String id,
    String name,
    String email,
    String phone,
    String role,
    String city,
    String pincode,
    String status,
    Instant createdAt
) {}
