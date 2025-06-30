package com.bookstore.order.dto;

import java.math.BigDecimal;

public record BookDto(
        Long id,
        String title,
        String author,
        String isbn,
        String genre,
        BigDecimal price,
        Integer stockQuantity,
        Long version
) {}
