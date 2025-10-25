package com.airport.flight;

import com.airport.flight.controller.FlightController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AirportFlightServiceApplicationTests {

	@Autowired
	private FlightController flightController;

	@Test
	void contextLoads() throws Exception {
		assertThat(flightController).isNotNull();
	}
}