package in.motorcircle.backend.identity.application;

import in.motorcircle.backend.dealer.domain.Dealer;
import in.motorcircle.backend.dealer.domain.DealerStatus;
import in.motorcircle.backend.dealer.infrastructure.DealerRepository;
import in.motorcircle.backend.identity.api.AuthSuccessResponse;
import in.motorcircle.backend.identity.api.SendOtpRequest;
import in.motorcircle.backend.identity.api.SendOtpResponse;
import in.motorcircle.backend.identity.api.UserProfileDto;
import in.motorcircle.backend.identity.api.VerifyOtpRequest;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.domain.UserStatus;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.shared.exception.DuplicateResourceException;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.security.JwtTokenProvider;
import in.motorcircle.backend.shared.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final DealerRepository dealerRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            OtpService otpService,
            UserRepository userRepository,
            DealerRepository dealerRepository,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.dealerRepository = dealerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public SendOtpResponse sendOtp(SendOtpRequest request) {
        boolean userExists = userRepository.findByPhone(request.phone()).isPresent();

        if ("LOGIN".equalsIgnoreCase(request.mode())) {
            if (!userExists) {
                throw new ResourceNotFoundException("No account found with mobile number " + request.phone() + ". Please register with us first.");
            }
        } else if ("REGISTER".equalsIgnoreCase(request.mode())) {
            if (userExists) {
                throw new DuplicateResourceException("An account is already registered with mobile number " + request.phone() + ". Please login.");
            }
        }

        int retryCooldown = otpService.sendOtp(request.phone());
        String devOtp = otpService.getLatestOtp(request.phone());
        return new SendOtpResponse(true, "OTP generated successfully for " + maskPhone(request.phone()), retryCooldown, devOtp);
    }

    @Transactional
    public AuthSuccessResponse verifyOtpAndAuthenticate(VerifyOtpRequest request) {
        otpService.verifyOtp(request.phone(), request.otp());

        // Find existing user or register new User in database
        java.util.Optional<User> existingUser = userRepository.findByPhone(request.phone());
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            if (request.name() == null || request.name().trim().isEmpty()) {
                throw new ResourceNotFoundException("No account found with mobile number " + request.phone() + ". Please register with us first.");
            }
            user = registerNewUser(request);
        }

        String dealerId = null;
        if (user.getRole() == UserRole.DEALER) {
            dealerId = dealerRepository.findByUserId(user.getId())
                    .map(Dealer::getId)
                    .orElseGet(() -> createDealerProfile(user, request));
        }

        UserPrincipal principal = UserPrincipal.create(user, dealerId);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        long expiresIn = jwtTokenProvider.getExpirationMinutes() * 60L;

        UserProfileDto profileDto = new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getCity(),
                user.getPincode(),
                dealerId
        );

        return AuthSuccessResponse.of(accessToken, expiresIn, profileDto);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getCity(),
                user.getPincode(),
                principal.getDealerId()
        );
    }

    private User registerNewUser(VerifyOtpRequest request) {
        String phone = request.phone();
        UserRole role = UserRole.BUYER;
        if (request.role() != null) {
            try {
                role = UserRole.valueOf(request.role().toUpperCase());
            } catch (Exception ignored) {}
        }

        String defaultName = role == UserRole.DEALER ? "Authorized Dealer" : "Car Buyer";
        String name = (request.name() != null && !request.name().trim().isEmpty())
                ? request.name().trim()
                : defaultName + " (" + phone.substring(Math.max(0, phone.length() - 4)) + ")";
        String city = (request.city() != null && !request.city().trim().isEmpty()) ? request.city().trim() : "Bengaluru";
        String email = role.name().toLowerCase() + "." + phone.replace("+", "") + "@motorcircle.in";

        User newUser = new User(
                UUID.randomUUID().toString(),
                name,
                email,
                phone,
                role,
                city,
                "560001",
                UserStatus.ACTIVE
        );
        return userRepository.save(newUser);
    }

    private String createDealerProfile(User user, VerifyOtpRequest request) {
        Dealer dealer = new Dealer();
        dealer.setId("dealer_" + UUID.randomUUID().toString().substring(0, 8));
        dealer.setUser(user);
        dealer.setDealershipName(request.dealershipName() != null && !request.dealershipName().trim().isEmpty()
                ? request.dealershipName().trim()
                : user.getName() + " Motors");
        dealer.setBrand("ALL");
        dealer.setCity(user.getCity() != null ? user.getCity() : "Pune");
        dealer.setAddress(user.getCity() != null ? user.getCity() + ", India" : "Pune, Maharashtra, India");
        dealer.setGstin("27AAACT" + (int)(Math.random() * 9000 + 1000) + "F1Z5");
        dealer.setStatus(DealerStatus.VERIFIED);
        dealer.setRating(new BigDecimal("4.90"));
        dealer.setTotalQuotes(0);
        dealer.setTotalLeadsDelivered(0);
        dealer.setVerifiedAt(Instant.now());
        dealerRepository.save(dealer);
        return dealer.getId();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, phone.length() - 4).replaceAll(".", "*") + phone.substring(phone.length() - 4);
    }
}
