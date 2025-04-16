package pe.edu.vallegrande.report_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "10000")
@TestPropertySource(properties = {
        "spring.r2dbc.url=r2dbc:postgresql://ep-broad-king-a5u5dwsk-pooler.us-east-2.aws.neon.tech:5432/users?sslmode=require",
        "spring.r2dbc.username=users_owner",
        "spring.r2dbc.password=npg_NFSkIrt3qA5z"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReportIntegrationTest {

    @Autowired
    private WebTestClient client;

    private static final String JWT = "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6Ijg1NzA4MWNhOWNiYjM3YzIzNDk4ZGQzOTQzYmYzNzFhMDU4ODNkMjgiLCJ0eXAiOiJKV1QifQ.eyJyb2xlIjoiQURNSU4iLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vc2VjdXJpdHktcHJzMSIsImF1ZCI6InNlY3VyaXR5LXByczEiLCJhdXRoX3RpbWUiOjE3NDQ3NTcxNjksInVzZXJfaWQiOiJQSmZaN1ZiSlhpY3RqUExKNHFha3h1UWNpcjgyIiwic3ViIjoiUEpmWjdWYkpYaWN0alBMSjRxYWt4dVFjaXI4MiIsImlhdCI6MTc0NDc1NzE2OSwiZXhwIjoxNzQ0NzYwNzY5LCJlbWFpbCI6ImFuZ2VsLmNhc3RpbGxhQHZhbGxlZ3JhbmRlLmVkdS5wZSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiZmlyZWJhc2UiOnsiaWRlbnRpdGllcyI6eyJlbWFpbCI6WyJhbmdlbC5jYXN0aWxsYUB2YWxsZWdyYW5kZS5lZHUucGUiXX0sInNpZ25faW5fcHJvdmlkZXIiOiJwYXNzd29yZCJ9fQ.fMy8LSqqjzTDRnVvCyyGyFUpAyu78O7rYp0Mt4n7aJT8h3Fvlf-cJCdiFgPLNqlpD9KeXUR-QKJdQ5OZYERqnRCinOvPYdDHW4ihmbtXj9IyS_GfDA1LmEta1NpNMunZAVMt4yqOXRQc3ivkdlqj1BfGW3Y7qifcgPeAifOyI6TRwARs6LDrW9Pup7yXJmELTzo_qMAJfeLIQ2wwmfiXGTc7cZgBnjGLAFLBczMOfnUYwJsrLaCT_Jqik85naSPIdo0sYbETOSQ2WgbIjgm2-UKbLbQYqGe8BcNblMAG3vs1O25EfiHscNqNaSBjzSQDoAXZgDdnETxhOtLYe95XzQ"; // tu token de Firebase válido

    @Test
    void getReports_shouldReturnOK() {
        client.get().uri("/api/reports")
                .header("Authorization", JWT)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }
}
