```java
package com.studyaws.documentdb.controller;

import com.studyaws.documentdb.security.JwtUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AuthControllerTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDbContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDbContainer::getReplicaSetUrl);
    }

    @Autowired
    private JwtUtil jwtUtil; // Para validar o token se necessário ou gerar tokens para outros testes

    @Test
    @DisplayName("Cenário de Sucesso: Login com credenciais válidas deve retornar 200 e um token JWT")
    void login_validCredentials_shouldReturnToken() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("username", "admin")
            .formParam("password", "admin")
        .when()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("token", notNullValue());
    }

    @Test
    @DisplayName("Cenário de Erro: Login com usuário inválido deve retornar 401")
    void login_invalidUsername_shouldReturnUnauthorized() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("username", "invalidUser")
            .formParam("password", "admin")
        .when()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body(equalTo("Usuário ou senha inválidos"));
    }

    @Test
    @DisplayName("Cenário de Erro: Login com senha inválida deve retornar 401")
    void login_invalidPassword_shouldReturnUnauthorized() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("username", "admin")
            .formParam("password", "invalidPass")
        .when()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body(equalTo("Usuário ou senha inválidos"));
    }

    @Test
    @DisplayName("Cenário de Erro: Login sem username deve retornar 400 (Bad Request)")
    void login_missingUsername_shouldReturnBadRequest() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("password", "admin")
        .when()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value()); // Spring Boot by default returns 400 for missing @RequestParam
    }

    @Test
    @DisplayName("Cenário de Erro: Login sem password deve retornar 400 (Bad Request)")
    void login_missingPassword_shouldReturnBadRequest() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("username", "admin")
        .when()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value()); // Spring Boot by default returns 400 for missing @RequestParam
    }

    @Test
    @DisplayName("Cenário de Erro: Campos vazios ou nulos para username/password devem retornar 401 (já que a validação é simples)")
    void login_emptyCredentials_shouldReturnUnauthorized() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("username", "")
            .formParam("password", "")
        .when()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body(equalTo("Usuário ou senha inválidos"));
    }
}
```