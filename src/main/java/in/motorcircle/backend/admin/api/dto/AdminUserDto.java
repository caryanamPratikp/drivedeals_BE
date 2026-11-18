package in.motorcircle.backend.admin.api.dto;

import java.time.Instant;
import java.util.List;

public record AdminUserDto(
    String id,
    String name,
    String email,
    String phone,
    String role,
    String city,
    String pincode,
    String status,
    Instant createdAt,
    String dealershipName,
    String dealershipBrand,
    List<UserJoinedGroupDto> joinedGroups
) {}
