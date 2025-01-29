package co.id.finease.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ConfigProperties {

    @Value("${app.secret-key}")
    private String secretKey;
}