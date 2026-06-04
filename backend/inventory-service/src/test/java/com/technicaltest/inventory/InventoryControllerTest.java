package com.technicaltest.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "service.api-key=test-key",
        "spring.datasource.url=jdbc:h2:mem:inventory-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerTest {
    private static final MediaType JSON_API = MediaType.valueOf(JsonApi.MEDIA_TYPE);
    private static final ProductSummary PRODUCT = new ProductSummary(
            "product-1",
            "Keyboard",
            BigDecimal.valueOf(100),
            "Mechanical keyboard"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @MockitoBean
    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        purchaseRepository.deleteAll();
        inventoryRepository.deleteAll();
        when(productClient.getProduct(PRODUCT.id())).thenReturn(PRODUCT);
    }

    @Test
    void exposesOpenApiWithoutApiKey() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("Inventory Service API")))
                .andExpect(jsonPath("$.components.securitySchemes.ApiKeyAuth.name", is("x-api-key")));
    }

    @Test
    void getsInventoryWithIncludedProduct() throws Exception {
        mockMvc.perform(get("/inventory/product-1").header("x-api-key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type", is("inventories")))
                .andExpect(jsonPath("$.data.attributes.quantity", is(0)))
                .andExpect(jsonPath("$.included[0].type", is("products")));
    }

    @Test
    void updatesInventory() throws Exception {
        mockMvc.perform(put("/inventory/product-1")
                        .header("x-api-key", "test-key")
                        .contentType(JSON_API)
                        .content("""
                                {
                                  "data": {
                                    "type": "inventories",
                                    "attributes": {
                                      "quantity": 8
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.quantity", is(8)));
    }

    @Test
    void createsPurchaseAndDiscountsInventory() throws Exception {
        inventoryRepository.save(new InventoryItem(PRODUCT.id(), 5));

        mockMvc.perform(post("/purchases")
                        .header("x-api-key", "test-key")
                        .contentType(JSON_API)
                        .content("""
                                {
                                  "data": {
                                    "type": "purchases",
                                    "attributes": {
                                      "productId": "product-1",
                                      "quantity": 2
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.attributes.total", is(200)))
                .andExpect(jsonPath("$.data.attributes.remainingQuantity", is(3)));
    }

    @Test
    void rejectsPurchaseWhenInventoryIsInsufficient() throws Exception {
        inventoryRepository.save(new InventoryItem(PRODUCT.id(), 1));

        mockMvc.perform(post("/purchases")
                        .header("x-api-key", "test-key")
                        .contentType(JSON_API)
                        .content("""
                                {
                                  "data": {
                                    "type": "purchases",
                                    "attributes": {
                                      "productId": "product-1",
                                      "quantity": 2
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].title", is("Insufficient inventory")));
    }

    @Test
    void validatesPurchasePayload() throws Exception {
        mockMvc.perform(post("/purchases")
                        .header("x-api-key", "test-key")
                        .contentType(JSON_API)
                        .content("""
                                {
                                  "data": {
                                    "type": "purchases",
                                    "attributes": {
                                      "productId": "",
                                      "quantity": 0
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].title", is("Invalid request")));
    }

    @Test
    void protectsEndpointsWithApiKey() throws Exception {
        mockMvc.perform(get("/inventory/product-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].title", is("Unauthorized")));
    }
}
