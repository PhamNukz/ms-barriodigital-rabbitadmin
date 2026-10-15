package cl.duoc.barriodigital.rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import cl.duoc.barriodigital.rabbitadmin.config.RabbitAdminProperties;

@SpringBootApplication(scanBasePackages = "cl.duoc.barriodigital")
@EnableConfigurationProperties(RabbitAdminProperties.class)
public class RabbitAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(RabbitAdminApplication.class, args);
    }
}
