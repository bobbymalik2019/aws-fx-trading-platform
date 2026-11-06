package com.charter26.fix_initiator_client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import com.charter26.fix_initiator_client.FixInitiatorClientApplication;

@SpringBootTest(classes = FixInitiatorClientApplication.class)
class FixInitiatorClientApplicationTests {

	@Test
	@Disabled("Skipping this specific test for now") // Disables just this method
	void contextLoads() {
	}

}
