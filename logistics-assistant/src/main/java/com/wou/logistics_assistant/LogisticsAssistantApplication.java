package com.wou.logistics_assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LogisticsAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogisticsAssistantApplication.class, args);
	}

}
