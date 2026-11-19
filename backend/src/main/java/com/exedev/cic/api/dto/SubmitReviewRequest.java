package com.exedev.cic.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SubmitReviewRequest(
        @NotBlank(message = "jobTitle is required")
        @Size(max = 140)
        String jobTitle,

        @NotBlank(message = "employmentStatus is required")
        @Pattern(regexp = "CURRENT_EMPLOYEE|FORMER_EMPLOYEE|CONTRACTOR",
                 message = "employmentStatus must be CURRENT_EMPLOYEE, FORMER_EMPLOYEE, or CONTRACTOR")
        String employmentStatus,

        @NotNull(message = "rating is required")
        @DecimalMin(value = "1.00")
        @DecimalMax(value = "5.00")
        BigDecimal rating,

        @NotNull(message = "cultureRating is required")
        @DecimalMin(value = "1.00")
        @DecimalMax(value = "5.00")
        BigDecimal cultureRating,

        @NotNull(message = "compensationRating is required")
        @DecimalMin(value = "1.00")
        @DecimalMax(value = "5.00")
        BigDecimal compensationRating,

        @NotBlank(message = "headline is required")
        @Size(min = 8, max = 240)
        String headline,

        @NotBlank(message = "pros is required")
        @Size(min = 15, max = 2000)
        String pros,

        @NotBlank(message = "cons is required")
        @Size(min = 15, max = 2000)
        String cons
) {}
