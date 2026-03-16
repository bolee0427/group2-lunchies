package com.bce.lunchies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestJooqConfig.class)
class LunchiesApplicationTests {

	@Test
	void contextLoads() {
	}

}
