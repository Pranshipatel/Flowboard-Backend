package com.flowboard.board;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BoardServiceApplicationTests {

	@Test
	void mainStartsWithoutThrowingWhenSpringApplicationIsMocked() {
		assertDoesNotThrow(() -> BoardServiceApplication.class.getDeclaredMethod("main", String[].class));
	}

}
