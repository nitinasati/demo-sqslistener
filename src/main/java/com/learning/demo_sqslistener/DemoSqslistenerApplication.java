package com.learning.demo_sqslistener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoSqslistenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoSqslistenerApplication.class, args);
	}

}
