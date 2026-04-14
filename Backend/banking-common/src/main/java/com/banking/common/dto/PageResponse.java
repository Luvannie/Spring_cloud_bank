package com.banking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response with content and page metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    
    /**
     * Creates a PageResponse from content, page metadata.
     */
    public static <T> PageResponse<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return PageResponse.<T>builder()
            .content(content)
            .pageNumber(pageNumber)
            .pageSize(pageSize)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .first(pageNumber == 0)
            .last(pageNumber >= totalPages - 1)
            .build();
    }
}
