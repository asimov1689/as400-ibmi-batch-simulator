package com.example.ibmi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ibmiOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("IBM i Portfolio Management API")
                                .version("2.0.0")
                                .description(
                                        "REST API layer over IBM i native programs via JT400 (IBM Toolbox for Java). "
                                                + "Demonstrates DB2 for i JDBC, *DTAQ (Data Queue), *PGM (Program Call), "
                                                + "and CL command execution against a live IBM i system.")
                                .contact(new Contact().name("Oliver Jaramillo")))
                .servers(
                        List.of(
                                new Server()
                                        .url("http://localhost:8080")
                                        .description("Local development")));
    }
}
