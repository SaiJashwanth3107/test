package com.example.first;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;

import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StudentCrudIntegrationTest {

    private static Long studentId;

    @BeforeAll
    static void setup() {
        // You can override this with -Dapi.baseUri=http://your-qa-server:8080
        String baseUri = "http://localhost:8081";
        RestAssured.baseURI = baseUri;
    }

    @Test
    @Order(1)
    void testCreateStudent() {
        String studentJson = "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}";
        ValidatableResponse response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(studentJson)
                .when()
                .post("/students")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("John Doe"))
                .body("email", equalTo("john@example.com"));

        studentId = response.extract().path("id");
        Assertions.assertNotNull(studentId, "Student ID should not be null after creation");
    }

    @Test
    @Order(2)
    void testGetStudentAfterCreate() {
        RestAssured.given()
                .when()
                .get("/students/{id}", studentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(studentId.intValue()))
                .body("name", equalTo("John Doe"))
                .body("email", equalTo("john@example.com"));
    }

    @Test
    @Order(3)
    void testUpdateStudent() {
        String updatedJson = "{\"name\":\"Jane Doe\",\"email\":\"jane@example.com\"}";
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(updatedJson)
                .when()
                .put("/students/{id}", studentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(studentId.intValue()))
                .body("name", equalTo("Jane Doe"))
                .body("email", equalTo("jane@example.com"));
    }

    @Test
    @Order(4)
    void testGetStudentAfterUpdate() {
        RestAssured.given()
                .when()
                .get("/students/{id}", studentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(studentId.intValue()))
                .body("name", equalTo("Jane Doe"))
                .body("email", equalTo("jane@example.com"));
    }

    @Test
    @Order(5)
    void testDeleteStudent() {
        RestAssured.given()
                .when()
                .delete("/students/{id}", studentId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(6)
    void testGetStudentAfterDelete() {
        RestAssured.given()
                .when()
                .get("/students/{id}", studentId)
                .then()
                .statusCode(404); // Assuming your API returns 404 for not found
    }
}

