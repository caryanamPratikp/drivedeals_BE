package in.motorcircle.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI motorcircleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MOTORCIRCLE Core REST API")
                        .description("Production REST API specification for MOTORCIRCLE — Indian group car-buying platform ('Buy smarter, together').")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("MOTORCIRCLE Engineering")
                                .email("engineering@motorcircle.in")
                                .url("https://motorcircle.in"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://motorcircle.in/terms")));
    }
}
