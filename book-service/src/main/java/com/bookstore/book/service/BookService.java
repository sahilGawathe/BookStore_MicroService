package com.bookstore.book.service;

import com.bookstore.book.dto.BookDtos.BookRequest;
import com.bookstore.book.entity.Book;
import com.bookstore.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found: " + id));
    }

    public List<Book> search(String query) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query);
    }

    @Transactional
    public Book create(BookRequest request) {
        Book book = Book.builder()
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .genre(request.genre())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .build();
        return bookRepository.save(book);
    }

    @Transactional
    public Book update(Long id, BookRequest request) {
        Book book = findById(id);
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setGenre(request.genre());
        book.setPrice(request.price());
        book.setStockQuantity(request.stockQuantity());
        return bookRepository.save(book);
    }

    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found: " + id);
        }
        bookRepository.deleteById(id);
    }

    /**
     * Adjusts stock atomically using JPA optimistic locking (@Version on Book).
     * Called internally by order-service when an order is placed or cancelled.
     * Negative delta = deduct (must not go below zero); positive = restock.
     */
    @Transactional
    public Book adjustStock(Long id, int delta) {
        Book book = findById(id);
        int newQuantity = book.getStockQuantity() + delta;
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Insufficient stock for book " + id + ": have " + book.getStockQuantity() + ", need " + (-delta));
        }
        book.setStockQuantity(newQuantity);
        return bookRepository.save(book);
    }
}
