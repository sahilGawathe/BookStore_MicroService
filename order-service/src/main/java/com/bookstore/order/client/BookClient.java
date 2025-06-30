package com.bookstore.order.client;

import com.bookstore.order.dto.BookDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Talks to book-service through Eureka (client-side load balancing via the
 * logical service name BOOK-SERVICE, resolved by spring-cloud-loadbalancer).
 */
@FeignClient(name = "BOOK-SERVICE")
public interface BookClient {

    @GetMapping("/api/books/{id}")
    BookDto getBook(@PathVariable("id") Long id);

    @PatchMapping("/api/books/{id}/stock")
    BookDto adjustStock(@PathVariable("id") Long id, @RequestBody StockAdjustRequest request);

    record StockAdjustRequest(Integer quantity) {}
}
