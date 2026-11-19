package com.exedev.cic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CicInsightsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CicInsightsApplication.class, args);
    }
}
