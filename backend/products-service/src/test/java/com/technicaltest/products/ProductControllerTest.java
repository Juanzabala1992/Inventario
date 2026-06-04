package com.technicaltest.products;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "service.api-key=test-key",
        "spring.datasource.url=jdbc:h2:mem:products-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {
    private static final MediaType JSON_API = MediaType.valueOf(JsonApi.MEDIA_TYPE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void returnsHealthWithoutApiKey() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is("products-service")))
                .andExpect(jsonPath("$.data.attributes.status", is("ok")));
    }

    @Test
    void exposesOpenApiWithoutApiKey() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("Products Service API")))
                .andExpect(jsonPath("$.components.securitySchemes.ApiKeyAuth.name", is("x-api-key")));
    }

    @Test
    void createsProductsUsingJsonApi() throws Exception {
        mockMvc.perform(post("/products")
                        .header("x-api-key", "test-key")
                        .contentType(JSON_API)
                        .content("""
                                {
                                  "data": {
                                    "type": "products",
                                    "attributes": {
                                      "name": "Keyboard",
                                      "price": 125.50,
                                      "description": "Mechanical keyboard"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Type", containsString(JsonApi.MEDIA_TYPE)))
                .andExpect(jsonPath("$.data.type", is("products")))
                .andExpect(jsonPath("$.data.attributes.name", is("Keyboard")));
    }

    @Test
    void getsProductById() throws Exception {
        Product product = productRepository.save(new Product(
                "Mouse",
                BigDecimal.valueOf(45),
                null
        ));

        mockMvc.perform(get("/products/{id}", product.getId()).header("x-api-key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(product.getId())))
                .andExpect(jsonPath("$.data.attributes.name", is("Mouse")))
                .andExpect(jsonPath("$.data.attributes.price", is(45.0)));
    }

    @Test
    void listsProducts() throws Exception {
        productRepository.save(new Product("Keyboard", BigDecimal.valueOf(100), null));
        productRepository.save(new Product("Mouse", BigDecimal.valueOf(45), "Wireless"));

        mockMvc.perform(get("/products").header("x-api-key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(2)));
    }

    @Test
    void protectsEndpointsWithApiKey() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].title", is("Unauthorized")));
    }

    @Test
    void returnsProductNotFoundAsJsonApiError() throws Exception {
        mockMvc.perform(get("/products/missing").header("x-api-key", "test-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].title", is("Not found")));
    }

    @Test
    void validatesCreateProductPayload() throws Exception {
        mockMvc.perform(post("/products")
                        .header("x-api-key", "test-key")
                        .contentType(JSON_API)
                        .content("""
                                {
                                  "data": {
                                    "type": "wrong",
                                    "attributes": {
                                      "name": "",
                                      "price": -1
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].title", is("Invalid request")));
    }
}
