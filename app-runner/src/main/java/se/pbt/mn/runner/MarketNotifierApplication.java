package se.pbt.mn.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Market Notifier application.
 */

@SpringBootApplication
@ComponentScan(basePackages = "se.pbt")
@EnableScheduling
public class MarketNotifierApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketNotifierApplication.class, args);
    }
}
