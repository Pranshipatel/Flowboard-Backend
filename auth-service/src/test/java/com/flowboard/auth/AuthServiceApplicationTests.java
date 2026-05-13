package com.flowboard.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class AuthServiceApplicationTests {

	@Test
	void applicationClassIsLoadable() {
		assertDoesNotThrow(() -> Class.forName(AuthServiceApplication.class.getName()));
	}

}
