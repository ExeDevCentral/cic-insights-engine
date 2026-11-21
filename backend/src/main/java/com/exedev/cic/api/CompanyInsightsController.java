package com.exedev.cic.api;

import com.exedev.cic.api.dto.CompanyInsightDto;
import com.exedev.cic.api.dto.CompanySuggestionDto;
import com.exedev.cic.api.dto.SearchFacetsDto;
import com.exedev.cic.api.dto.SubmitReviewRequest;
import com.exedev.cic.search.CompanySearchCriteria;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@CrossOrigin(origins = "*")
@Tag(name = "Company Insights & Search", description = "Search, facets, typeahead suggestions, and review aggregation")
public class CompanyInsightsController {

    private final CompanyInsightsService insightsService;

    public CompanyInsightsController(CompanyInsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping
    @Operation(summary = "Search and filter company profiles with composable criteria")
    public ResponseEntity<Page<CompanyInsightDto>> searchCompanies(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Boolean verifiedOnly,
            @RequestParam(required = false) String headquarters,
            @RequestParam(required = false) Integer minActiveJobs,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "seoMonthlyImpressions", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        CompanySearchCriteria criteria = new CompanySearchCriteria(
                industry, minRating, verifiedOnly, headquarters, minActiveJobs, q
        );
        return ResponseEntity.ok(insightsService.searchCompanies(criteria, pageable));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Prefix-ranked autocomplete suggestions for the search bar")
    public ResponseEntity<List<CompanySuggestionDto>> suggestCompanies(
            @RequestParam String q,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(insightsService.suggestCompanies(q, limit));
    }

    @GetMapping("/facets")
    @Operation(summary = "Aggregated industry counts and global summary for search filters")
    public ResponseEntity<SearchFacetsDto> getSearchFacets() {
        return ResponseEntity.ok(insightsService.getSearchFacets());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a company profile by slug with Schema.org JSON-LD metadata")
    public ResponseEntity<CompanyInsightDto> getCompanyBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(insightsService.getBySlug(slug));
    }

    @PostMapping("/{slug}/reviews")
    @Operation(summary = "Submit an employee review and update O(1) aggregate scores")
    public ResponseEntity<CompanyInsightDto> submitReview(
            @PathVariable String slug,
            @Valid @RequestBody SubmitReviewRequest request
    ) {
        CompanyInsightDto updated = insightsService.submitReview(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }
}
