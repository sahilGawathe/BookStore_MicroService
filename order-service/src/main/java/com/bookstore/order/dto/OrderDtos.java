package com.bookstore.order.dto;

import com.bookstore.order.entity.Order;
import com.bookstore.order.entity.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    public record OrderItemRequest(
            @NotNull Long bookId,
            @NotNull @Min(1) Integer quantity
    ) {}

    public record PlaceOrderRequest(
            @NotEmpty(message = "Order must contain at least one item")
            @Valid
            List<OrderItemRequest> items
    ) {}

    public record OrderItemResponse(
            Long bookId,
            String bookTitle,
            BigDecimal priceAtPurchase,
            Integer quantity
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getBookId(), item.getBookTitle(), item.getPriceAtPurchase(), item.getQuantity());
        }
    }

    public record OrderResponse(
            Long id,
            Long userId,
            List<OrderItemResponse> items,
            BigDecimal totalAmount,
            String status,
            LocalDateTime createdAt
    ) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.getId(),
                    order.getUserId(),
                    order.getItems().stream().map(OrderItemResponse::from).toList(),
                    order.getTotalAmount(),
                    order.getStatus().name(),
                    order.getCreatedAt()
            );
        }
    }

    public record ErrorResponse(String error) {}
}
