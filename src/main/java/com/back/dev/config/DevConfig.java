package com.back.dev.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(("!prod"))
@Configuration
public class DevConfig {
}
