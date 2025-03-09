package com.learning.demo_sqslistener;

import com.learning.demo_sqslistener.config.TestAwsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = DemoSqslistenerApplication.class)
@Import({TestAwsConfig.class})
@TestPropertySource(locations = "classpath:application.properties")
class DemoSqslistenerApplicationTests {

	@Test
	void contextLoads() {
	}

}
