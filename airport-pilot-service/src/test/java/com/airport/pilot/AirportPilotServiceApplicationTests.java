package com.airport.pilot;

import com.airport.pilot.controller.PilotController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AirportPilotServiceApplicationTests {

	@Autowired
	private PilotController pilotController;

	@Test
	void contextLoads() throws Exception {
		assertThat(pilotController).isNotNull();
	}
}