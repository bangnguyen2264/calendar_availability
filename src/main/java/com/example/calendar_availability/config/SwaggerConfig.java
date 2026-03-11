package com.example.calendar_availability.config;


import com.example.calendar_availability.utils.AppUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {
    private final AppUtils appUtils;

    @Bean
    public OpenAPI customizeOpenAPI() {
        Contact contact = new Contact();
        contact.setName(appUtils.getContactName());
        contact.setEmail(appUtils.getContactEmail());

        return new OpenAPI()
                .info(new Info()
                        .title(appUtils.getTitle())
                        .description(appUtils.getDescription())
                        .version(appUtils.getVersion())
                        .contact(contact));
    }

}