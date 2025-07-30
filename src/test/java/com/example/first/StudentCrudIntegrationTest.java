package com.example.first;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;

import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StudentCrudIntegrationTest {

    private static String studentId;

    @BeforeAll
    static void setup() {
        // You can override this with -Dapi.baseUri=http://your-qa-server:8080
        String baseUri = "http://localhost:8081";
        RestAssured.baseURI = baseUri;
    }

    @Test
    @Order(1)
    void testCreateStudent() {
        // Create a student
        String studentJson = "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}";
        ValidatableResponse response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(studentJson)
                .when()
                .post("/students")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("John Doe"))
                .body("email", equalTo("john@example.com"));

        studentId = response.extract().path("id");
        Assertions.assertNotNull(studentId, "Student ID should not be null after creation");
    }

    @Test
    @Order(2)
    void testGetStudentAfterCreate() {
        // Verify the student after creation
        RestAssured.given()
                .when()
                .get("/students/{id}", studentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(studentId))
                .body("name", equalTo("John Doe"))
                .body("email", equalTo("john@example.com"));
    }

    @Test
    @Order(3)
    void testUpdateStudent() {
        // Update the student's details
        String updatedJson = "{\"name\":\"Jane Doe\",\"email\":\"jane@example.com\"}";
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(updatedJson)
                .when()
                .put("/students/{id}", studentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(studentId))
                .body("name", equalTo("Jane Doe"))
                .body("email", equalTo("jane@example.com"));
    }

    @Test
    @Order(4)
    void testGetStudentAfterUpdate() {
        // Verify the student after the update
        RestAssured.given()
                .when()
                .get("/students/{id}", studentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(studentId))
                .body("name", equalTo("Jane Doe"))
                .body("email", equalTo("jane@example.com"));
    }

    @Test
    @Order(5)
    void testDeleteStudent() {
        // Delete the student
        RestAssured.given()
                .when()
                .delete("/students/{id}", studentId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(6)
    void testGetStudentAfterDelete() {
        // Verify the student after deletion (should return 404)
        RestAssured.given()
                .when()
                .get("/students/{id}", studentId)
                .then()
                .statusCode(404); // Assuming your API returns 404 for not found
    }

    // --- EDGE CASE TESTS ---

    @Test
    @Order(7)
    void testPost_NullFields() {
        // Test null fields in POST request
        String body = "{ \"name\": null, \"email\": null }";
        RestAssured.given().contentType(ContentType.JSON).body(body)
                .when().post("/students")
                .then().statusCode(400);
    }

    @Test
    @Order(8)
    void testPost_EmptyStrings() {
        // Test empty strings in POST request
        String body = "{ \"name\": \"\", \"email\": \"\" }";
        RestAssured.given().contentType(ContentType.JSON).body(body)
                .when().post("/students")
                .then().statusCode(400);
    }

    @Test
    @Order(9)
    void testPost_LongNameAndEmail() {
        // Test long name and email in POST request
        String longName = "A".repeat(300);
        String longEmail = "a".repeat(250) + "@example.com";
        String body = String.format("{ \"name\": \"%s\", \"email\": \"%s\" }", longName, longEmail);

        RestAssured.given().contentType(ContentType.JSON).body(body)
                .when().post("/students")
                .then().statusCode(anyOf(is(400), is(413)));
    }

    @Test
    @Order(10)
    void testPost_SQLInjectionAttempt() {
        // Test SQL injection attempt in POST request
        String body = "{ \"name\": \"Robert'); DROP TABLE students;--\", \"email\": \"evil@example.com\" }";
        RestAssured.given().contentType(ContentType.JSON).body(body)
                .when().post("/students")
                .then().statusCode(400);
    }

    @Test
    @Order(11)
    void testGet_SQLInjectionEmail() {
        // Test SQL injection in email query parameter
        RestAssured.given().queryParam("email", "abc@example.com' OR '1'='1")
                .when().get("/students")
                .then().statusCode(400);
    }

    @Test
    @Order(12)
    void testGet_LongEmailParam() {
        // Test long email query parameter
        String longEmail = "a".repeat(255) + "@example.com";
        RestAssured.given().queryParam("email", longEmail)
                .when().get("/students")
                .then().statusCode(anyOf(is(404), is(400)));
    }

    @Test
    @Order(13)
    void testPut_EmptyName() {
        // Test empty name in PUT request
        String body = "{ \"name\": \"\", \"email\": \"test@example.com\" }";
        RestAssured.given().contentType(ContentType.JSON).body(body)
                .when().put("/students/{id}", studentId)
                .then().statusCode(400);
    }

    @Test
    @Order(14)
    void testPut_SQLInjectionInName() {
        // Test SQL injection in name field of PUT request
        String body = "{ \"name\": \"' OR 1=1 --\", \"email\": \"test@example.com\" }";
        RestAssured.given().contentType(ContentType.JSON).body(body)
                .when().put("/students/{id}", studentId)
                .then().statusCode(400);
    }

    @Test
    @Order(15)
    void testDelete_SQLInjection() {
        // Test SQL injection in DELETE query parameter
        RestAssured.given().queryParam("email", "'; DROP TABLE students; --")
                .when().delete("/students")
                .then().statusCode(400);
    }

    @Test
    @Order(16)
    void testDelete_EmptyEmail() {
        // Test empty email in DELETE query parameter
        RestAssured.given().queryParam("email", "")
                .when().delete("/students")
                .then().statusCode(405);
    }
}
