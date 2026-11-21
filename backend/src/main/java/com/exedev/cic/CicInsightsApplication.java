package com.exedev.cic;

import com.exedev.cic.config.CicProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(CicProperties.class)
public class CicInsightsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CicInsightsApplication.class, args);
    }
}
