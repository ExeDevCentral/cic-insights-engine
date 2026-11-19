package com.exedev.cic.api;

import com.exedev.cic.api.dto.CompanyInsightDto;
import com.exedev.cic.api.dto.SubmitReviewRequest;
import com.exedev.cic.service.CompanyInsightsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanyInsightsController.class)
class CompanyInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyInsightsService insightsService;

    @Test
    void shouldReturnCompanyProfileWithJsonLdWhenSlugExists() throws Exception {
        CompanyInsightDto dto = new CompanyInsightDto(
                UUID.randomUUID(),
                "indeed-global",
                "Indeed",
                "HR Tech & Search",
                "Austin, TX",
                "10,000+",
                new BigDecimal("4.62"),
                new BigDecimal("4.70"),
                new BigDecimal("4.55"),
                1284,
                342,
                new BigDecimal("7.85"),
                18_450_000L,
                true,
                Instant.now(),
                Map.of("@type", "EmployerAggregateRating", "ratingValue", 4.62)
        );

        when(insightsService.getBySlug("indeed-global")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/companies/indeed-global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("indeed-global"))
                .andExpect(jsonPath("$.overallRating").value(4.62))
                .andExpect(jsonPath("$.jsonLdStructuredData['@type']").value("EmployerAggregateRating"));
    }

    @Test
    void shouldReturnRfc7807ProblemDetailWhenCompanyNotFound() throws Exception {
        when(insightsService.getBySlug("unknown-co"))
                .thenThrow(new EntityNotFoundException("Company not found for slug: unknown-co"));

        mockMvc.perform(get("/api/v1/companies/unknown-co"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn400WithFieldErrorsWhenReviewPayloadIsInvalid() throws Exception {
        SubmitReviewRequest invalidPayload = new SubmitReviewRequest(
                "",
                "INVALID_STATUS",
                new BigDecimal("6.00"),
                new BigDecimal("4.00"),
                new BigDecimal("4.00"),
                "Short",
                "Too short",
                "Too short"
        );

        mockMvc.perform(post("/api/v1/companies/indeed-global/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.fieldErrors.employmentStatus").exists())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
    }
}
