package com.example.ibmi.config;

import com.ibm.as400.access.AS400;
import java.beans.PropertyVetoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IbmiConnectionConfig {

    @Value("${ibmi.host}")
    private String host;

    @Value("${ibmi.user}")
    private String user;

    @Value("${ibmi.password}")
    private String password;

    @Value("${ibmi.library}")
    private String library;

    @Bean
    public AS400 as400() throws PropertyVetoException {
        AS400 as400 = new AS400(host, user, password);
        as400.setGuiAvailable(false);
        return as400;
    }

    @Bean
    public String ibmiLibrary() {
        return library;
    }
}
