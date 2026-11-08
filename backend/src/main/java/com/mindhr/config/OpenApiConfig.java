package com.mindhr.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI mindHrOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("mindHR API")
                        .version("0.0.1")
                        .description("Multi-tenant HR / workforce management SaaS. Call POST /api/auth/login, "
                                + "then click Authorize and paste the returned token. Platform staff must pass "
                                + "organizationId on org-owned resources; org users are scoped automatically.\n\n"
                                + "**QA logins** (test data, password `QaTest@123`): `qa.owner@mindhr.test` (owner), "
                                + "`qa.lead@mindhr.test` (team lead), `qa.emp1@mindhr.test` and `qa.emp2@mindhr.test` "
                                + "(employees). They are also in the Examples list of POST /api/auth/login."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
