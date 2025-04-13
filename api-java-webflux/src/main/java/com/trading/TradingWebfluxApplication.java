package com.trading;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class TradingWebfluxApplication {
    public static void main(String... args) {
        log.info("Staring Application: {}", TradingWebfluxApplication.class.getSimpleName());
        SpringApplication.run(TradingWebfluxApplication.class, args);
    }
}
