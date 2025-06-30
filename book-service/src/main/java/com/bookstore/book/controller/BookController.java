package com.bookstore.book.controller;

import com.bookstore.book.dto.BookDtos.BookRequest;
import com.bookstore.book.dto.BookDtos.StockAdjustRequest;
import com.bookstore.book.entity.Book;
import com.bookstore.book.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<Book> getAll() {
        return bookService.findAll();
    }

    @GetMapping("/{id}")
    public Book getById(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @GetMapping("/search")
    public List<Book> search(@RequestParam String query) {
        return bookService.search(query);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> create(@Valid @RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Book update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Called by order-service (server-to-server) to deduct/restock inventory.
    // Trusts the caller's own auth; in production this would be locked to
    // internal traffic only (e.g. via network policy or a service-to-service token).
    @PatchMapping("/{id}/stock")
    public Book adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest request) {
        return bookService.adjustStock(id, request.quantity());
    }
}
