package com.rohit.url_shortner.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Regression guard: /docs is a reserved word in the RedirectController regex.
    // Without it, the shortCode mapping would swallow this path and return 404.
    @Test
    void docsPathRedirectsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("/swagger-ui/**"));
    }

    @Test
    void openApiSpecIsServedAndDescribesTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("URL Shortener API"))
                .andExpect(jsonPath("$.paths./api/urls.post").exists())
                .andExpect(jsonPath("$.paths./api/urls/{shortCode}/stats.get").exists());
    }

    @Test
    void reservedWordsAreNotTreatedAsShortCodes() throws Exception {
        // "docs" resolves to Swagger (302 to the UI), never to the shortener's 404
        mockMvc.perform(get("/docs")).andExpect(status().isFound());
        // a non-reserved unknown code still gets the shortener's 404 semantics
        mockMvc.perform(get("/zzzzzz")).andExpect(status().isNotFound());
    }
}
