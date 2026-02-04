package com.ccnu.military;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 军事通信效能评估系统 - 主启动类
 * 
 * @author CCNU
 * @version 1.0.0
 */
@SpringBootApplication
public class MilitaryCommunicationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MilitaryCommunicationApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("军事通信效能评估系统启动成功！");
        System.out.println("访问地址: http://localhost:8080/api");
        System.out.println("API文档: http://localhost:8080/api/swagger-ui.html");
        System.out.println("========================================\n");
    }
}
