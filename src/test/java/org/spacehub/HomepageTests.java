package org.spacehub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = { Homepage.class, TestSecurityConfig.class },
  properties = "spring.main.allow-bean-definition-overriding=true"
)
@Import(TestSecurityConfig.class)
class HomepageTests {

  @Test
  void contextLoads() {
  }
}
