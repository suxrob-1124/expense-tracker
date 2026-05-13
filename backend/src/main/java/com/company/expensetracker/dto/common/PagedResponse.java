package com.company.expensetracker.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper used by all list endpoints.
 *
 * <p>Wraps a Spring Data {@link Page} into a stable JSON shape so that
 * clients do not depend on Spring's internal page representation.
 *
 * @param <T> the type of element in the page
 */
@Schema(description = "A single page of results with pagination metadata.")
public record PagedResponse<T>(

        @Schema(description = "Items on the current page.")
        List<T> content,

        @Schema(description = "Zero-based page index.", example = "0")
        int page,

        @Schema(description = "Maximum number of items per page.", example = "20")
        int size,

        @Schema(description = "Total number of items across all pages.", example = "42")
        long totalElements,

        @Schema(description = "Total number of pages.", example = "3")
        int totalPages,

        @Schema(description = "Whether this is the last page.", example = "false")
        boolean last
) {
    /**
     * Constructs a {@code PagedResponse} from a Spring Data {@link Page}.
     *
     * @param <T> the element type
     * @param p   the Spring Data page to convert
     * @return a {@code PagedResponse} containing the page content and metadata
     */
    public static <T> PagedResponse<T> from(Page<T> p) {
        return new PagedResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isLast()
        );
    }
}
