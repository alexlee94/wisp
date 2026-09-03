package com.wisp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WispApplication {
	public static void main(String[] args) {
		SpringApplication.run(WispApplication.class, args);
	}
}
