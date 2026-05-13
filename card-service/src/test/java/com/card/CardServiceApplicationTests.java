package com.card;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class CardServiceApplicationTests {

	@Test
	void applicationClassIsLoadable() {
		assertDoesNotThrow(() -> Class.forName(CardServiceApplication.class.getName()));
	}

}
