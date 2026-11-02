package com.falsegod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 觉醒伪神 · 登录注册后端启动类
 */
@SpringBootApplication
public class FalseGodApplication {

    public static void main(String[] args) {
        SpringApplication.run(FalseGodApplication.class, args);
        System.out.println("\n  伪神后端已启动 -> http://localhost:8080\n");
    }
}
