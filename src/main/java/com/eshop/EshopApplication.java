package com.eshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类 — 类比 Python 的 uvicorn.run(app)
 */
@SpringBootApplication
public class EshopApplication {
    public static void main(String[] args) {
        SpringApplication.run(EshopApplication.class, args);
    }
}
