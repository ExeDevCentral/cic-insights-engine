package com.exedev.cic.api;

import com.exedev.cic.api.dto.CompanyInsightDto;
import com.exedev.cic.api.dto.CompanySuggestionDto;
import com.exedev.cic.api.dto.SearchFacetsDto;
import com.exedev.cic.api.dto.SubmitReviewRequest;
import com.exedev.cic.domain.CompanyNotFoundException;
import com.exedev.cic.service.CompanyInsightsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyInsightsController.class)
class CompanyInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyInsightsService insightsService;

    @Test
    void returnsCompanyProfileWithJsonLdWhenSlugExists() throws Exception {
        CompanyInsightDto dto = new CompanyInsightDto(
                UUID.randomUUID(),
                "acme-cloud",
                "Acme Cloud",
                "Cloud Infrastructure",
                "Austin, TX",
                "1,000+",
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

        when(insightsService.getBySlug("acme-cloud")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/companies/acme-cloud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("acme-cloud"))
                .andExpect(jsonPath("$.overallRating").value(4.62))
                .andExpect(jsonPath("$.jsonLdStructuredData['@type']").value("EmployerAggregateRating"));
    }

    @Test
    void returnsSuggestionsForTypeaheadQuery() throws Exception {
        when(insightsService.suggestCompanies("ac", 5)).thenReturn(List.of(
                new CompanySuggestionDto("acme-cloud", "Acme Cloud", "Cloud", new BigDecimal("4.62"), true)
        ));

        mockMvc.perform(get("/api/v1/companies/suggest").param("q", "ac").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("acme-cloud"))
                .andExpect(jsonPath("$[0].verifiedEmployer").value(true));
    }

    @Test
    void returnsSearchFacets() throws Exception {
        when(insightsService.getSearchFacets()).thenReturn(new SearchFacetsDto(
                12L, 9L, new BigDecimal("4.42"), Map.of("Cloud", 5L, "Fintech", 4L)
        ));

        mockMvc.perform(get("/api/v1/companies/facets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompanies").value(12))
                .andExpect(jsonPath("$.countByIndustry.Cloud").value(5));
    }

    @Test
    void returns400WhenSearchMinRatingIsOutOfBounds() throws Exception {
        mockMvc.perform(get("/api/v1/companies").param("minRating", "9.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Query Parameter"));
    }

    @Test
    void returnsRfc7807ProblemDetailWhenCompanyNotFound() throws Exception {
        when(insightsService.getBySlug("unknown-co"))
                .thenThrow(new CompanyNotFoundException("unknown-co"));

        mockMvc.perform(get("/api/v1/companies/unknown-co"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.slug").value("unknown-co"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returns409WhenOptimisticLockConflictOccurs() throws Exception {
        SubmitReviewRequest validPayload = new SubmitReviewRequest(
                "Backend Engineer",
                "CURRENT_EMPLOYEE",
                new BigDecimal("4.50"),
                new BigDecimal("4.40"),
                new BigDecimal("4.60"),
                "Solid architecture and team",
                "Good testing standards and clear ownership",
                "Legacy services still need gradual cleanup"
        );

        when(insightsService.submitReview(eq("acme-cloud"), any(SubmitReviewRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("CompanyProfile", "acme-cloud"));

        mockMvc.perform(post("/api/v1/companies/acme-cloud/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent Update Conflict"));
    }

    @Test
    void returns400WithFieldErrorsWhenReviewPayloadIsInvalid() throws Exception {
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

        mockMvc.perform(post("/api/v1/companies/acme-cloud/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.fieldErrors.employmentStatus").exists())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
    }
}
