package in.motorcircle.backend.identity.domain;

public enum UserRole {
    BUYER,
    DEALER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
