package in.motorcircle.backend.admin.api.dto;

import java.time.Instant;

public record UserJoinedGroupDto(
    String groupId,
    String carModel,
    String variantName,
    String city,
    String stage,
    int currentMembers,
    int targetMembers,
    Instant joinedAt
) {}
