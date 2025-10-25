package com.airport.checkin;

import com.airport.checkin.controller.CheckinController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AirportCheckinServiceApplicationTests {

	@Autowired
	private CheckinController checkinController;

	@Test
	void contextLoads() throws Exception {
		assertThat(checkinController).isNotNull();
	}
}