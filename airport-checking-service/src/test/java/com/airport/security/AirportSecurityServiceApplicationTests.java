package com.airport.security;

import com.airport.security.controller.SecurityController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AirportSecurityServiceApplicationTests {

	@Autowired
	private SecurityController securityController;

	@Test
	void contextLoads() throws Exception {
		assertThat(securityController).isNotNull();
	}

}
