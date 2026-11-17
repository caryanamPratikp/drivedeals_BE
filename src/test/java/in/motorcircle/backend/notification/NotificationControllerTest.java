package in.motorcircle.backend.notification;

import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.domain.UserStatus;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.notification.application.NotificationService;
import in.motorcircle.backend.notification.domain.Notification;
import in.motorcircle.backend.shared.security.JwtTokenProvider;
import in.motorcircle.backend.shared.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user;
    private String token;
    private Notification notification;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User(
                "usr_notif_test", "Amitabh Bachan", "amitabh@test.com", "+919833344455",
                UserRole.BUYER, "Mumbai", "400049", UserStatus.ACTIVE
        ));

        token = jwtTokenProvider.generateAccessToken(UserPrincipal.create(user, null));

        notification = notificationService.createNotification(
                user,
                "Quote Revision Available",
                "A dealer revised their quotation on your group demand",
                "QUOTE_REVISED",
                "/quotes"
        );
    }

    @Test
    @DisplayName("Authenticated user can retrieve their notifications")
    void testGetNotifications() throws Exception {
        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(notification.getId()))
                .andExpect(jsonPath("$.data[0].title").value("Quote Revision Available"));
    }

    @Test
    @DisplayName("Authenticated user can mark notification as read")
    void testMarkNotificationRead() throws Exception {
        mockMvc.perform(patch("/notifications/" + notification.getId() + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
