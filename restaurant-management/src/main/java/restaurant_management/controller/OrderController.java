package restaurant_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import restaurant_management.dto.OrderRequest;
import restaurant_management.dto.OrderResponse;
import restaurant_management.service.OrderService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;


    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    // ==========================================================
    // PLACE NEW ORDER
    // ==========================================================

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody OrderRequest request) {

        return ResponseEntity.ok(
                orderService.createOrder(request)
        );
    }


    // ==========================================================
    // DELETE WHOLE ORDER
    // ==========================================================

    // DELETE:
    // /api/orders/id/{orderId}

    @DeleteMapping("/id/{orderId}")
    public ResponseEntity<Void> deleteWholeOrder(
            @PathVariable Integer orderId) {

        orderService.deleteWholeOrder(orderId);

        return ResponseEntity.noContent().build();
    }


    // ==========================================================
    // GET ORDER BY ORDER NUMBER
    // ==========================================================

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String orderNumber) {

        return ResponseEntity.ok(
                orderService.getOrderByNumber(orderNumber)
        );
    }


    // ==========================================================
    // GET ALL ORDERS
    // ==========================================================

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {

        return ResponseEntity.ok(
                orderService.getAllOrders()
        );
    }


    // ==========================================================
    // UPDATE PAYMENT STATUS
    // ==========================================================

    @PatchMapping("/{orderNumber}/payment")
    public ResponseEntity<OrderResponse> updatePayment(
            @PathVariable String orderNumber,
            @RequestBody Map<String, String> request) {

        String paymentStatus = request.get("paymentStatus");
        String paymentMethod = request.get("paymentMethod");

        if (paymentStatus == null || paymentStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                orderService.updatePayment(
                        orderNumber,
                        paymentStatus,
                        paymentMethod));
    }


    // ==========================================================
    // UPDATE WHOLE ORDER STATUS
    // ==========================================================

    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String orderNumber,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        if (status == null ||
                status.trim().isEmpty()) {

            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                orderService.updateOrderStatus(
                        orderNumber,
                        status
                )
        );
    }


    // ==========================================================
    // UPDATE ITEM STATUS USING ORDER ITEM ID
    // ==========================================================
    //
    // PATCH:
    // /api/orders/items/{orderItemId}/status
    //
    // Example:
    // /api/orders/items/15/status
    //
    // Body:
    // {
    //     "status": "PREPARING"
    // }
    //
    // ==========================================================

    @PatchMapping("/items/{orderItemId}/status")
    public ResponseEntity<OrderResponse> updateItemStatusDirect(
            @PathVariable Integer orderItemId,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        if (status == null ||
                status.trim().isEmpty()) {

            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                orderService.updateOrderItemStatusDirect(
                        orderItemId,
                        status
                )
        );
    }


    // ==========================================================
    // UPDATE ITEM STATUS - OLD/KITCHEN ENDPOINT
    // ==========================================================
    //
    // This is kept so Bakery, Beverages and Main Kitchen
    // continue working.
    //
    // PATCH:
    // /api/orders/ORD-00001/items/15/status
    //
    // ==========================================================

    @PatchMapping("/{orderNumber}/items/{orderItemId}/status")
    public ResponseEntity<OrderResponse> updateOrderItemStatus(
            @PathVariable String orderNumber,
            @PathVariable Integer orderItemId,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        if (status == null ||
                status.trim().isEmpty()) {

            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                orderService.updateOrderItemStatus(
                        orderNumber,
                        orderItemId,
                        status
                )
        );
    }
}