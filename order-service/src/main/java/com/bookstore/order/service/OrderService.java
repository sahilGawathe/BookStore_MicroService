package com.bookstore.order.service;

import com.bookstore.order.client.BookClient;
import com.bookstore.order.client.BookClient.StockAdjustRequest;
import com.bookstore.order.dto.BookDto;
import com.bookstore.order.dto.OrderDtos.OrderItemRequest;
import com.bookstore.order.dto.OrderDtos.PlaceOrderRequest;
import com.bookstore.order.entity.Order;
import com.bookstore.order.entity.Order.OrderStatus;
import com.bookstore.order.entity.OrderItem;
import com.bookstore.order.repository.OrderRepository;
import com.bookstore.order.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookClient bookClient;

    /**
     * Places an order:
     *  1. Look up each book (price + availability) from book-service.
     *  2. Deduct stock for each line item via book-service.
     *  3. Persist the order with a price snapshot.
     * If any stock deduction fails partway through, previously-deducted
     * items are restocked so we don't leave book-service's inventory short.
     * (A real system would use a saga/outbox instead of this best-effort
     * compensation — see README for the honest limitations.)
     */
    @Transactional
    public Order placeOrder(PlaceOrderRequest request) {
        Long userId = CurrentUser.id();
        List<OrderItem> deductedSoFar = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        try {
            for (OrderItemRequest itemReq : request.items()) {
                BookDto book = bookClient.getBook(itemReq.bookId());
                if (book == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found: " + itemReq.bookId());
                }

                // Deduct stock now (negative delta) — book-service rejects if insufficient
                bookClient.adjustStock(itemReq.bookId(), new StockAdjustRequest(-itemReq.quantity()));

                OrderItem orderItem = OrderItem.builder()
                        .bookId(book.id())
                        .bookTitle(book.title())
                        .priceAtPurchase(book.price())
                        .quantity(itemReq.quantity())
                        .build();

                orderItems.add(orderItem);
                deductedSoFar.add(orderItem);
                total = total.add(book.price().multiply(BigDecimal.valueOf(itemReq.quantity())));
            }
        } catch (Exception ex) {
            // Compensate: restock anything we already deducted before the failure
            for (OrderItem deducted : deductedSoFar) {
                try {
                    bookClient.adjustStock(deducted.getBookId(), new StockAdjustRequest(deducted.getQuantity()));
                } catch (Exception restockEx) {
                    log.error("CRITICAL: failed to restock bookId={} qty={} after order failure — manual reconciliation needed",
                            deducted.getBookId(), deducted.getQuantity(), restockEx);
                }
            }
            if (ex instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach book-service: " + ex.getMessage());
        }

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(total)
                .status(OrderStatus.PLACED)
                .build();
        orderItems.forEach(order::addItem);

        return orderRepository.save(order);
    }

    public List<Order> getMyOrders() {
        return orderRepository.findByUserId(CurrentUser.id());
    }

    public Order getById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        if (!order.getUserId().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this order");
        }
        return order;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getById(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already cancelled");
        }

        // Restock every item
        for (OrderItem item : order.getItems()) {
            bookClient.adjustStock(item.getBookId(), new StockAdjustRequest(item.getQuantity()));
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
