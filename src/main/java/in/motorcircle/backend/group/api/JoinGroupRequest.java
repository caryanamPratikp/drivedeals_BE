package in.motorcircle.backend.group.api;

import jakarta.validation.Valid;

public record JoinGroupRequest(
    @Valid
    CreateSignalPayload signal
) {}
