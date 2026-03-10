package com.lypzis.lead_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.lypzis.lead_domain.entity")
@EnableJpaRepositories(basePackages = "com.lypzis.lead_domain.repository")
public class LeadWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeadWorkerApplication.class, args);
	}

}
