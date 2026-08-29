package com.qbank;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智能题库管理系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.qbank.mapper")
public class QbankApplication {

    public static void main(String[] args) {
        SpringApplication.run(QbankApplication.class, args);
    }
}
