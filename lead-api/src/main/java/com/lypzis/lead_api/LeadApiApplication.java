package com.lypzis.lead_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.lypzis.lead_domain.entity")
@EnableJpaRepositories(basePackages = {
		"com.lypzis.lead_api.repository",
		"com.lypzis.lead_domain.repository"
})
public class LeadApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeadApiApplication.class, args);
	}

}
