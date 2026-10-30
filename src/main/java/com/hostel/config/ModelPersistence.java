package com.hostel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

public final class ModelPersistence {
    private ModelPersistence() {}
    @Configuration
    @Profile("!model49")
    @EntityScan("com.hostel.entity")
    @EnableJpaRepositories("com.hostel.repository")
    public static class HistoricalV10 {}

    @Configuration
    @Profile("model49")
    @EntityScan("com.hostel.model49.entity")
    @EnableJpaRepositories("com.hostel.model49.repository")
    public static class ApprovedV11 {}
}
