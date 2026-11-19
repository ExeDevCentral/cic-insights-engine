package com.exedev.cic.api;

import com.exedev.cic.api.dto.CompanyInsightDto;
import com.exedev.cic.api.dto.SubmitReviewRequest;
import com.exedev.cic.service.CompanyInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/companies")
@CrossOrigin(origins = "*")
@Tag(name = "Company Insights & SEO Telemetry", description = "Endpoints for CIC Employer Branding & SEO Marketing Insights")
public class CompanyInsightsController {

    private final CompanyInsightsService insightsService;

    public CompanyInsightsController(CompanyInsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping
    @Operation(summary = "Search and filter companies with SEO and reputation metrics")
    public ResponseEntity<Page<CompanyInsightDto>> searchCompanies(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "seoMonthlyImpressions", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(insightsService.searchCompanies(industry, minRating, q, pageable));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get company profile by SEO slug including JSON-LD Schema.org metadata")
    public ResponseEntity<CompanyInsightDto> getCompanyBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(insightsService.getBySlug(slug));
    }

    @PostMapping("/{slug}/reviews")
    @Operation(summary = "Submit a verified employer review and recompute O(1) aggregate scores")
    public ResponseEntity<CompanyInsightDto> submitReview(
            @PathVariable String slug,
            @Valid @RequestBody SubmitReviewRequest request
    ) {
        CompanyInsightDto updated = insightsService.submitReview(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }
}
