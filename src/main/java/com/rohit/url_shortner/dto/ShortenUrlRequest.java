package com.rohit.url_shortner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShortenUrlRequest(

        @NotBlank(message = "url must not be blank")
        @Size(max = 2048, message = "url must not exceed 2048 characters")
        String url,

        @Min(value = 1, message = "expiryDays must be at least 1")
        @Max(value = 365, message = "expiryDays must not exceed 365")
        Integer expiryDays
) {
}
