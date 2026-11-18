package in.motorcircle.backend.shared.security;

import in.motorcircle.backend.identity.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private final String id;
    private final String phone;
    private final String name;
    private final String role;
    private final String dealerId;
    private final String dealershipName;
    private final String brand;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String id, String phone, String name, String role, String dealerId, String dealershipName, String brand) {
        this.id = id;
        this.phone = phone;
        this.name = name;
        this.role = role;
        this.dealerId = dealerId;
        this.dealershipName = dealershipName;
        this.brand = brand;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
    }

    public UserPrincipal(String id, String phone, String name, String role, String dealerId) {
        this(id, phone, name, role, dealerId, null, null);
    }

    public static UserPrincipal create(User user, String dealerId) {
        return new UserPrincipal(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getRole().getAuthority(),
                dealerId,
                null,
                null
        );
    }

    public static UserPrincipal create(User user, String dealerId, String dealershipName, String brand) {
        return new UserPrincipal(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getRole().getAuthority(),
                dealerId,
                dealershipName,
                brand
        );
    }

    public String getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getDealerId() {
        return dealerId;
    }

    public String getDealershipName() {
        return dealershipName;
    }

    public String getBrand() {
        return brand;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return ""; // Passwordless
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
