package com.bce.lunchies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LunchiesApplication {

	public static void main(String[] args) {
		SpringApplication.run(LunchiesApplication.class, args);
    System.out.println("🥳Lunchies Application Started🥳");
	}

}
