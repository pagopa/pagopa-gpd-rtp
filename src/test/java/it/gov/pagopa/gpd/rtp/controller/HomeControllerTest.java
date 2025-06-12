package it.gov.pagopa.gpd.rtp.controller;

import it.gov.pagopa.gpd.rtp.model.AppInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {HomeController.class})
class HomeControllerTest {
    @Value("${info.application.name}")
    private String name;

    @Value("${info.application.version}")
    private String version;

    @Value("${info.properties.environment}")
    private String environment;
    @Autowired
    private HomeController sut;

    @Test
    void home_OK(){
        assertDoesNotThrow(() -> sut.home());
    }

    @Test
    void healthCheck_OK(){
        ResponseEntity<AppInfo> response = assertDoesNotThrow(() -> sut.healthCheck());
        assertEquals(name, response.getBody().getName());
        assertEquals(version, response.getBody().getVersion());
        assertEquals(environment, response.getBody().getEnvironment());
    }
}