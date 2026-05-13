package com.eureka;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EurekaServerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainStartsSpringApplication() {
		String[] args = new String[0];

		try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
			EurekaServerApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(EurekaServerApplication.class, args));
		}
	}

}
