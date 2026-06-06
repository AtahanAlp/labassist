package com.labassist;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "labassist.lab-device.polling-enabled=false")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
