package com.bookstore.book.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BookDtos {

    public record BookRequest(
            @NotBlank String title,
            @NotBlank String author,
            String isbn,
            String genre,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
            @NotNull @Min(0) Integer stockQuantity
    ) {}

    public record StockAdjustRequest(
            @NotNull Integer quantity // positive to add, negative to deduct
    ) {}

    public record ErrorResponse(String error) {}
}
