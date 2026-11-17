package in.motorcircle.backend.identity.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class TextBeeSmsSender {

    private static final Logger log = LoggerFactory.getLogger(TextBeeSmsSender.class);
    private static final String TEXTBEE_URL = "https://api.textbee.dev/api/v1/gateway/send-sms";

    @Value("${motorcircle.sms.textbee-api-key:txb_0cSGWclNsMwHU6TxfOVPe09FjeCAjca8}")
    private String apiKey;

    @Value("${motorcircle.sms.enabled:true}")
    private boolean enabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean sendOtpSms(String phone, String otp) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.info("[TEXTBEE_SMS] SMS dispatch disabled or API key missing. Skipping external SMS.");
            return false;
        }

        try {
            // Format phone to E.164 (ensure leading +)
            String recipient = phone.startsWith("+") ? phone : "+" + phone;
            String message = String.format("Your DriveDeals verification OTP code is: %s. Valid for 5 minutes.", otp);

            String jsonPayload = String.format(
                    "{\"recipients\":[\"%s\"],\"message\":\"%s\"}",
                    recipient,
                    escapeJson(message)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TEXTBEE_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[TEXTBEE_SMS] Real SMS dispatched successfully to [{}]. Response: {}", maskPhone(phone), response.body());
                return true;
            } else {
                log.warn("[TEXTBEE_SMS] Failed to dispatch SMS to [{}]. Status: {}, Response: {}",
                        maskPhone(phone), response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[TEXTBEE_SMS] Exception while sending SMS to [{}]: {}", maskPhone(phone), e.getMessage());
            return false;
        }
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, phone.length() - 4).replaceAll(".", "*") + phone.substring(phone.length() - 4);
    }
}
