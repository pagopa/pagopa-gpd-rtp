package it.gov.pagopa.microservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ApplicationTest {

    @Test
    void contextLoads() {
        // check only if the context is loaded
        assertTrue(true);
    }
}
