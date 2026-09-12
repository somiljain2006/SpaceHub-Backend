package org.spacehub;

/**
 * HomepageTests.java
 *
 * Basic test class for verifying that the Spring Boot application context loads correctly
 * in the "test" profile for the SpaceHub application.
 */

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HomepageTests {
  // testing in real lifee
  @Test
  void contextLoads() {
  }

}