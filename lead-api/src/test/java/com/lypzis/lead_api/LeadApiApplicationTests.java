package com.lypzis.lead_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:leadapi;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"JWT_SECRET=test_secret_key_with_32_chars_minimum_123",
		"WHATSAPP_VERIFY_TOKEN=test_verify_token",
		"spring.data.redis.host=localhost",
		"spring.data.redis.port=6379"
})
class LeadApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
