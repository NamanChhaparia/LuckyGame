package com.gaming.luckengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Luck and Reward Engine.
 * This is a real-time luck-based gaming service with strict budget management.
 * 
 * @author Gaming Team
 * @version 1.0
 */
@SpringBootApplication
@EnableScheduling
public class LuckRewardEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(LuckRewardEngineApplication.class, args);
    }
}

