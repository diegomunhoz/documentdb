```java
package com.studyaws.documentdb.controller;

import com.studyaws.documentdb.dto.ProdutoRequest;
import com.studyaws.documentdb.model.Produto;
import com.studyaws.documentdb.repository.ProdutoRepository;
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

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class ProdutoControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String validToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        produtoRepository.deleteAll(); // Limpa o banco de dados antes de cada teste
        validToken = "Bearer " + jwtUtil.generateToken("admin");
    }

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDbContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDbContainer::getReplicaSetUrl);
    }

    // --- GET /api/produtos ---
    @Test
    @DisplayName("GET /api/produtos - Cenário de Sucesso: Deve retornar lista vazia quando não há produtos")
    void listarProdutos_emptyList_shouldReturn200AndEmptyArray() {
        given()
            .header("Authorization", validToken)
        .when()
            .get("/api/produtos")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("", empty());
    }

    @Test
    @DisplayName("GET /api/produtos - Cenário de Sucesso: Deve retornar todos os produtos")
    void listarProdutos_withProducts_shouldReturn200AndProducts() {
        produtoRepository.save(new Produto() {{ setNome("Produto A"); setPreco(10.0); setEstoque(100); }});
        produtoRepository.save(new Produto() {{ setNome("Produto B"); setPreco(20.0); setEstoque(50); }});

        given()
            .header("Authorization", validToken)
        .when()
            .get("/api/produtos")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("size()", is(2))
            .body("[0].nome", anyOf(equalTo("Produto A"), equalTo("Produto B")))
            .body("[1].nome", anyOf(equalTo("Produto A"), equalTo("Produto B")));
    }

    @Test
    @DisplayName("GET /api/produtos - Cenário de Autorização: Deve retornar 401 para requisição sem token")
    void listarProdutos_noToken_shouldReturnUnauthorized() {
        given()
        .when()
            .get("/api/produtos")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("GET /api/produtos - Cenário de Autorização: Deve retornar 401 para requisição com token inválido")
    void listarProdutos_invalidToken_shouldReturnUnauthorized() {
        given()
            .header("Authorization", "Bearer invalid.token.string")
        .when()
            .get("/api/produtos")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    // --- GET /api/produtos/{id} ---
    @Test
    @DisplayName("GET /api/produtos/{id} - Cenário de Sucesso: Deve retornar um produto por ID")
    void buscarProdutoPorId_validId_shouldReturn200AndProduct() {
        Produto produto = produtoRepository.save(new Produto() {{ setNome("Produto Teste"); setPreco(15.50); setEstoque(20); }});

        given()
            .header("Authorization", validToken)
        .when()
            .get("/api/produtos/{id}", produto.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(produto.getId()))
            .body("nome", equalTo("Produto Teste"))
            .body("preco", equalTo(15.5F)) // RestAssured converte Double para Float por padrão em some()
            .body("estoque", equalTo(20));
    }

    @Test
    @DisplayName("GET /api/produtos/{id} - Cenário de Erro: Deve retornar 404 para ID não encontrado")
    void buscarProdutoPorId_notFoundId_shouldReturn404() {
        given()
            .header("Authorization", validToken)
        .when()
            .get("/api/produtos/{id}", "naoExiste123")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("error", equalTo("Produto não encontrado"));
    }

    @Test
    @DisplayName("GET /api/produtos/{id} - Cenário de Erro: Deve retornar 400 para ID inválido (formato que não seja String válida para MongoDB ObjectId ou que cause erro na busca)")
    void buscarProdutoPorId_malformedId_shouldReturn500() { // MongoDB geralmente retorna 500 para IDs malformados que não são 24 hex chars
        given()
            .header("Authorization", validToken)
        .when()
            .get("/api/produtos/{id}", "malformedId")
        .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()) // Ou 400 dependendo da validação interna do driver/Spring
            .body("error", containsString("Erro interno")); // Mensagem genérica do ApiExceptionHandler
    }


    // --- POST /api/produtos ---
    @Test
    @DisplayName("POST /api/produtos - Cenário de Sucesso: Deve criar um novo produto")
    void criarProduto_validRequest_shouldReturn201AndProduct() {
        ProdutoRequest request = new ProdutoRequest("Novo Produto", 25.0, 30);

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/produtos")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("nome", equalTo("Novo Produto"))
            .body("preco", equalTo(25.0F))
            .body("estoque", equalTo(30));

        // Verifica se foi salvo no banco de dados
        List<Produto> produtos = produtoRepository.findAll();
        assertThat(produtos).hasSize(1);
        assertThat(produtos.get(0).getNome()).isEqualTo("Novo Produto");
    }

    @Test
    @DisplayName("POST /api/produtos - Cenário de Erro: Deve retornar 400 para nome em branco")
    void criarProduto_blankName_shouldReturn400() {
        ProdutoRequest request = new ProdutoRequest("", 25.0, 30); // Nome em branco

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/produtos")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("nome", equalTo("não deve estar em branco"));
    }

    @Test
    @DisplayName("POST /api/produtos - Cenário de Erro: Deve retornar 400 para nome nulo")
    void criarProduto_nullName_shouldReturn400() {
        // Usar um Map para construir o JSON e forçar o campo nulo, pois record é final
        String requestBody = "{\"nome\":null, \"preco\":25.0, \"estoque\":30}";

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/produtos")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("nome", equalTo("não deve estar em branco"));
    }


    @Test
    @DisplayName("POST /api/produtos - Cenário de Erro: Deve retornar 400 para preço nulo")
    void criarProduto_nullPrice_shouldReturn400() {
        String requestBody = "{\"nome\":\"Produto Invalido\", \"preco\":null, \"estoque\":30}";

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/produtos")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("preco", equalTo("não deve ser nulo"));
    }

    @Test
    @DisplayName("POST /api/produtos - Cenário de Erro: Deve retornar 400 para preço negativo")
    void criarProduto_negativePrice_shouldReturn400() {
        ProdutoRequest request = new ProdutoRequest("Produto Invalido", -10.0, 30);

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/produtos")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("preco", equalTo("deve ser maior que 0"));
    }

    @Test
    @DisplayName("POST /api/produtos - Cenário de Erro: Deve retornar 400 para estoque nulo")
    void criarProduto_nullStock_shouldReturn400() {
        String requestBody = "{\"nome\":\"Produto Invalido\", \"preco\":10.0, \"estoque\":null}";

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/produtos")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("estoque", equalTo("não deve ser nulo"));
    }

    @Test
    @DisplayName("POST /api/produtos - Cenário de Erro: Deve retornar 400 para estoque negativo")
    void criarProduto_negativeStock_shouldReturn400() {
        ProdutoRequest request = new ProdutoRequest("Produto Invalido", 10.0, -5);

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/produtos")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("estoque", equalTo("deve ser maior ou igual a 0"));
    }

    // --- PUT /api/produtos/{id} ---
    @Test
    @DisplayName("PUT /api/produtos/{id} - Cenário de Sucesso: Deve atualizar um produto existente")
    void atualizarProduto_validIdAndRequest_shouldReturn200AndUpdatedProduct() {
        Produto produtoSalvo = produtoRepository.save(new Produto() {{ setNome("Produto Antigo"); setPreco(10.0); setEstoque(100); }});
        ProdutoRequest request = new ProdutoRequest("Produto Atualizado", 30.0, 70);

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/produtos/{id}", produtoSalvo.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(produtoSalvo.getId()))
            .body("nome", equalTo("Produto Atualizado"))
            .body("preco", equalTo(30.0F))
            .body("estoque", equalTo(70));

        // Verifica no banco
        Produto produtoAtualizado = produtoRepository.findById(produtoSalvo.getId()).orElseThrow();
        assertThat(produtoAtualizado.getNome()).isEqualTo("Produto Atualizado");
        assertThat(produtoAtualizado.getPreco()).isEqualTo(30.0);
        assertThat(produtoAtualizado.getEstoque()).isEqualTo(70);
    }

    @Test
    @DisplayName("PUT /api/produtos/{id} - Cenário de Erro: Deve retornar 404 para ID não encontrado")
    void atualizarProduto_notFoundId_shouldReturn404() {
        ProdutoRequest request = new ProdutoRequest("Produto Atualizado", 30.0, 70);

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/produtos/{id}", "naoExiste123")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("error", equalTo("Produto não encontrado"));
    }

    @Test
    @DisplayName("PUT /api/produtos/{id} - Cenário de Erro: Deve retornar 400 para nome em branco na atualização")
    void atualizarProduto_blankName_shouldReturn400() {
        Produto produtoSalvo = produtoRepository.save(new Produto() {{ setNome("Produto Antigo"); setPreco(10.0); setEstoque(100); }});
        ProdutoRequest request = new ProdutoRequest("", 30.0, 70);

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/produtos/{id}", produtoSalvo.getId())
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("nome", equalTo("não deve estar em branco"));
    }

    // Outros casos de erro de validação para PUT (preço nulo, negativo, estoque nulo, negativo) seriam semelhantes aos do POST
    @Test
    @DisplayName("PUT /api/produtos/{id} - Cenário de Erro: Deve retornar 400 para preço negativo na atualização")
    void atualizarProduto_negativePrice_shouldReturn400() {
        Produto produtoSalvo = produtoRepository.save(new Produto() {{ setNome("Produto Antigo"); setPreco(10.0); setEstoque(100); }});
        ProdutoRequest request = new ProdutoRequest("Produto Atualizado", -5.0, 70);

        given()
            .header("Authorization", validToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/produtos/{id}", produtoSalvo.getId())
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("preco", equalTo("deve ser maior que 0"));
    }

    // --- DELETE /api/produtos/{id} ---
    @Test
    @DisplayName("DELETE /api/produtos/{id} - Cenário de Sucesso: Deve deletar um produto existente")
    void deletarProduto_validId_shouldReturn204() {
        Produto produtoSalvo = produtoRepository.save(new Produto() {{ setNome("Produto a Deletar"); setPreco(5.0); setEstoque(10); }});

        given()
            .header("Authorization", validToken)
        .when()
            .delete("/api/produtos/{id}", produtoSalvo.getId())
        .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Verifica se foi removido do banco de dados
        assertThat(produtoRepository.findById(produtoSalvo.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/produtos/{id} - Cenário de Erro: Deve retornar 404 para ID não encontrado")
    void deletarProduto_notFoundId_shouldReturn404() {
        given()
            .header("Authorization", validToken)
        .when()
            .delete("/api/produtos/{id}", "naoExiste123")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("error", equalTo("Produto não encontrado"));
    }

    @Test
    @DisplayName("DELETE /api/produtos/{id} - Cenário de Erro: Deve retornar 500 para ID malformado")
    void deletarProduto_malformedId_shouldReturn500() {
        given()
            .header("Authorization", validToken)
        .when()
            .delete("/api/produtos/{id}", "malformedId")
        .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .body("error", containsString("Erro interno"));
    }
}
```