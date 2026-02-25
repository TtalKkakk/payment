package com.example.pg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = "com.example.pg")
@EnableJpaRepositories(basePackages = "com.example.pg")
public class PgApplication {
	public static void main(String[] args) {
		SpringApplication.run(PgApplication.class, args);
	}

}
