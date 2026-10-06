package restaurant_management.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import restaurant_management.dto.OrderItemRequest;
import restaurant_management.dto.OrderRequest;
import restaurant_management.dto.OrderResponse;
import restaurant_management.entity.Customer;
import restaurant_management.entity.MenuItem;
import restaurant_management.entity.OrderItem;
import restaurant_management.entity.RestaurantOrder;
import restaurant_management.repository.CustomerRepository;
import restaurant_management.repository.MenuItemRepository;
import restaurant_management.repository.OrderItemRepository;
import restaurant_management.repository.OrderRepository;
import restaurant_management.entity.TableReservation;
import restaurant_management.repository.TableReservationRepository;

@Service
public class OrderService {

    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TableReservationRepository tableReservationRepository;


    public OrderService(
            CustomerRepository customerRepository,
            MenuItemRepository menuItemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            TableReservationRepository tableReservationRepository) {

        this.customerRepository = customerRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableReservationRepository = tableReservationRepository;
    }


    // ============================================================
    // CREATE ORDER
    // ============================================================

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        if (request.getCustomerName() == null ||
                request.getCustomerName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Customer name is required");
        }


        if (request.getPhone() == null ||
                request.getPhone().trim().isEmpty()) {

            throw new RuntimeException(
                    "Phone number is required");
        }


        if (request.getItems() == null ||
                request.getItems().isEmpty()) {

            throw new RuntimeException(
                    "Order must contain at least one item");
        }


        // --------------------------------------------------------
        // FIND OR CREATE CUSTOMER
        // --------------------------------------------------------

        Customer customer = customerRepository
                .findByPhone(request.getPhone().trim())
                .orElseGet(() -> {

                    Customer newCustomer = new Customer();

                    newCustomer.setName(
                            request.getCustomerName().trim());

                    newCustomer.setPhone(
                            request.getPhone().trim());

                    return customerRepository.save(newCustomer);
                });


        // --------------------------------------------------------
        // CREATE ORDER
        // --------------------------------------------------------

        RestaurantOrder order =
                new RestaurantOrder();

        LocalDate today =
                LocalDate.now();

        LocalDateTime now =
                LocalDateTime.now();


        order.setCustomer(customer);

        order.setOrderType(
                request.getOrderType());

        order.setRoomType(
                request.getRoomType());

        order.setStatus("PENDING");

        order.setCreatedAt(now);

        order.setOrderDate(today);
        order.setPaymentStatus("PENDING");
        order.setPaymentMethod("NOT_SELECTED");
        order.setPaymentTime(null);

        // --------------------------------------------------------
        // OPTIONAL TABLE BOOKING CHARGE
        // --------------------------------------------------------

        double bookingCharge = 0.0;
        String reservationNumber = null;

        if (request.getReservationNumber() != null &&
                !request.getReservationNumber().trim().isEmpty()) {

            reservationNumber = request.getReservationNumber().trim().toUpperCase();

            if (!"Dine In".equalsIgnoreCase(request.getOrderType())) {
                throw new RuntimeException(
                        "A table reservation can only be linked to a Dine In order");
            }

            if (orderRepository.existsByReservationNumber(reservationNumber)) {
                throw new RuntimeException(
                        "This table reservation has already been linked to an order");
            }

            final String linkedReservationNumber = reservationNumber;

            TableReservation reservation =
                    tableReservationRepository.findByReservationNumber(linkedReservationNumber)
                            .orElseThrow(() -> new RuntimeException(
                                    "Reservation not found: " + linkedReservationNumber));

            if (!request.getPhone().trim().equals(reservation.getPhone())) {
                throw new RuntimeException(
                        "Reservation phone number does not match the order phone number");
            }

            if ("CANCELLED".equalsIgnoreCase(reservation.getStatus()) ||
                    "COMPLETED".equalsIgnoreCase(reservation.getStatus())) {
                throw new RuntimeException(
                        "This reservation is no longer active");
            }

            bookingCharge = 50.0;
            order.setReservationNumber(linkedReservationNumber);
        }

        order.setBookingCharge(bookingCharge);


        // --------------------------------------------------------
        // DAILY ORDER NUMBER
        // --------------------------------------------------------

        Integer nextSequence = 1;

        var lastOrder =
                orderRepository
                        .findTopByOrderDateOrderByDailySequenceDesc(
                                today);

        if (lastOrder.isPresent()) {

            nextSequence =
                    lastOrder.get()
                            .getDailySequence() + 1;
        }


        order.setDailySequence(
                nextSequence);

        order.setOrderNumber(
                "ORD-" +
                String.format(
                        "%05d",
                        nextSequence
                ));


        // --------------------------------------------------------
        // CREATE ORDER ITEMS
        // --------------------------------------------------------

        double foodSubtotal = 0.0;

        List<OrderItem> orderItems =
                new ArrayList<>();


        for (OrderItemRequest itemRequest :
                request.getItems()) {

            if (itemRequest.getQuantity() == null ||
                    itemRequest.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Quantity must be greater than zero");
            }


            MenuItem menuItem =
                    menuItemRepository
                            .findById(
                                    itemRequest.getItemId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Menu item not found: "
                                            + itemRequest.getItemId()));

            if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
                throw new RuntimeException(
                        menuItem.getDishName() + " is currently out of stock");
            }


            double price =
                    menuItem.getPrice();

            int quantity =
                    itemRequest.getQuantity();

            double subtotal =
                    price * quantity;


            OrderItem orderItem =
                    new OrderItem();


            orderItem.setOrder(order);

            orderItem.setMenuItem(menuItem);

            orderItem.setDishName(
                    menuItem.getDishName());

            orderItem.setPrice(price);

            orderItem.setQuantity(quantity);

            orderItem.setSubtotal(subtotal);

            orderItem.setStatus("PENDING");


            orderItems.add(orderItem);

            foodSubtotal += subtotal;
        }


        // --------------------------------------------------------
        // ROOM CHARGE
        // --------------------------------------------------------

        double roomCharge = 0.0;


        if ("Dine In".equalsIgnoreCase(
                request.getOrderType())) {

            if ("AC".equalsIgnoreCase(
                    request.getRoomType())) {

                roomCharge = 100.0;

            } else if ("Non-AC".equalsIgnoreCase(
                    request.getRoomType())) {

                roomCharge = 50.0;
            }
        }


        // --------------------------------------------------------
        // GST
        // --------------------------------------------------------

        double taxableAmount =
                foodSubtotal + roomCharge + bookingCharge;

        double gst =
                taxableAmount * 0.05;


        // --------------------------------------------------------
        // TOTAL
        // --------------------------------------------------------

        double totalAmount =
                taxableAmount + gst;


        order.setFoodSubtotal(
                foodSubtotal);

        order.setRoomCharge(
                roomCharge);

        order.setBookingCharge(bookingCharge);

        order.setGst(gst);

        order.setTotalAmount(
                totalAmount);

        order.setItems(orderItems);


        // --------------------------------------------------------
        // SAVE ORDER
        // --------------------------------------------------------

        RestaurantOrder savedOrder =
                orderRepository.save(order);


        return convertToResponse(savedOrder);
    }


    // ============================================================
    // GET ORDER BY ORDER NUMBER
    // ============================================================

   public OrderResponse getOrderByNumber(
        String orderNumber) {

    LocalDate today = LocalDate.now();

    RestaurantOrder order =
            orderRepository
                    .findByOrderNumberAndOrderDate(
                            orderNumber,
                            today
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Order not found"));

    return convertToResponse(order);
}


    // ============================================================
    // GET ALL ORDERS
    // ============================================================

    public List<OrderResponse> getAllOrders() {

        List<RestaurantOrder> orders =
                orderRepository.findAll();

        List<OrderResponse> response =
                new ArrayList<>();


        for (RestaurantOrder order :
                orders) {

            response.add(
                    convertToResponse(order));
        }


        return response;
    }


    // ============================================================
    // UPDATE WHOLE ORDER STATUS
    // ============================================================

    @Transactional
    public OrderResponse updateOrderStatus(
            String orderNumber,
            String status) {

        if (status == null ||
                status.trim().isEmpty()) {

            throw new RuntimeException(
                    "Status is required");
        }


        RestaurantOrder order =
                orderRepository
                        .findByOrderNumber(orderNumber)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"));


        order.setStatus(
                status.trim().toUpperCase());


        RestaurantOrder savedOrder =
                orderRepository.save(order);


        return convertToResponse(savedOrder);
    }


    // ============================================================
    // DELETE WHOLE ORDER
    // ============================================================

    @Transactional
    public void deleteWholeOrder(Integer orderId) {

        if (orderId == null) {
            throw new RuntimeException(
                    "Order ID is required");
        }

        RestaurantOrder order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found: " + orderId));

        // OrderItem records are deleted automatically because
        // RestaurantOrder uses CascadeType.ALL + orphanRemoval=true.
        orderRepository.delete(order);
    }


    // ============================================================
    // UPDATE ORDER ITEM QUANTITY
    // ============================================================

    @Transactional
    public OrderResponse updateOrderItemQuantity(
            Integer orderItemId,
            Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException(
                    "Quantity must be greater than zero");
        }

        OrderItem orderItem =
                orderItemRepository
                        .findById(orderItemId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order item not found: "
                                                + orderItemId));

        RestaurantOrder order = orderItem.getOrder();

        if (order == null) {
            throw new RuntimeException(
                    "Order item is not linked to an order");
        }

        if ("COMPLETED".equalsIgnoreCase(orderItem.getStatus())) {
            throw new RuntimeException(
                    "Completed items cannot be modified");
        }

        double price = orderItem.getPrice();
        double subtotal = price * quantity;

        orderItem.setQuantity(quantity);
        orderItem.setSubtotal(subtotal);

        orderItemRepository.save(orderItem);

        recalculateOrderTotals(order);
        updateOverallOrderStatus(order);

        return convertToResponse(order);
    }


    // ============================================================
    // REMOVE ORDER ITEM
    // ============================================================

    @Transactional
    public OrderResponse removeOrderItem(
            Integer orderItemId) {

        OrderItem orderItem =
                orderItemRepository
                        .findById(orderItemId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order item not found: "
                                                + orderItemId));

        RestaurantOrder order = orderItem.getOrder();

        if (order == null) {
            throw new RuntimeException(
                    "Order item is not linked to an order");
        }

        if ("COMPLETED".equalsIgnoreCase(orderItem.getStatus())) {
            throw new RuntimeException(
                    "Completed items cannot be removed");
        }

        if (order.getItems() == null || order.getItems().size() <= 1) {
            throw new RuntimeException(
                    "Order must contain at least one item");
        }

        order.getItems().remove(orderItem);

        recalculateOrderTotals(order);
        updateOverallOrderStatus(order);

        return convertToResponse(order);
    }


    // ============================================================
    // RECALCULATE ORDER TOTALS
    // ============================================================

    private void recalculateOrderTotals(
            RestaurantOrder order) {

        double foodSubtotal = 0.0;

        if (order.getItems() != null) {

            for (OrderItem item : order.getItems()) {

                if (item.getPrice() == null ||
                        item.getQuantity() == null) {
                    continue;
                }

                double subtotal =
                        item.getPrice() * item.getQuantity();

                item.setSubtotal(subtotal);

                foodSubtotal += subtotal;
            }
        }

        double roomCharge = 0.0;

        if ("Dine In".equalsIgnoreCase(order.getOrderType())) {

            if ("AC".equalsIgnoreCase(order.getRoomType())) {
                roomCharge = 100.0;

            } else if ("Non-AC".equalsIgnoreCase(order.getRoomType())) {
                roomCharge = 50.0;
            }
        }

        double bookingCharge = order.getBookingCharge() == null
                ? 0.0
                : order.getBookingCharge();

        double taxableAmount = foodSubtotal + roomCharge + bookingCharge;
        double gst = taxableAmount * 0.05;
        double totalAmount = taxableAmount + gst;

        order.setFoodSubtotal(foodSubtotal);
        order.setRoomCharge(roomCharge);
        order.setBookingCharge(bookingCharge);
        order.setGst(gst);
        order.setTotalAmount(totalAmount);

        orderRepository.save(order);
    }


    // ============================================================
    // UPDATE ITEM STATUS DIRECTLY USING ORDER ITEM ID
    // ============================================================

    @Transactional
    public OrderResponse updateOrderItemStatusDirect(
            Integer orderItemId,
            String status) {

        if (status == null ||
                status.trim().isEmpty()) {

            throw new RuntimeException(
                    "Status is required");
        }


        String cleanStatus =
                status.trim().toUpperCase();


        // --------------------------------------------------------
        // VALID STATUS
        // --------------------------------------------------------

        if (!cleanStatus.equals("PENDING") &&
                !cleanStatus.equals("PREPARING") &&
                !cleanStatus.equals("COMPLETED")) {

            throw new RuntimeException(
                    "Invalid status. Use PENDING, PREPARING or COMPLETED");
        }


        // --------------------------------------------------------
        // FIND ITEM DIRECTLY
        // --------------------------------------------------------

        OrderItem orderItem =
                orderItemRepository
                        .findById(orderItemId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order item not found: "
                                        + orderItemId));


        // --------------------------------------------------------
        // GET PARENT ORDER
        // --------------------------------------------------------

        RestaurantOrder order =
                orderItem.getOrder();


        if (order == null) {

            throw new RuntimeException(
                    "Order item is not linked to an order");
        }


        // --------------------------------------------------------
        // UPDATE ITEM
        // --------------------------------------------------------

        orderItem.setStatus(
                cleanStatus);

        orderItemRepository.save(
                orderItem);


        // --------------------------------------------------------
        // UPDATE OVERALL ORDER STATUS
        // --------------------------------------------------------

        updateOverallOrderStatus(order);


        return convertToResponse(order);
    }


    // ============================================================
    // UPDATE ITEM STATUS - KITCHEN COMPATIBILITY
    // ============================================================

    @Transactional
    public OrderResponse updateOrderItemStatus(
            String orderNumber,
            Integer orderItemId,
            String status) {

        if (status == null ||
                status.trim().isEmpty()) {

            throw new RuntimeException(
                    "Status is required");
        }


        String cleanStatus =
                status.trim().toUpperCase();


        if (!cleanStatus.equals("PENDING") &&
                !cleanStatus.equals("PREPARING") &&
                !cleanStatus.equals("COMPLETED")) {

            throw new RuntimeException(
                    "Invalid status. Use PENDING, PREPARING or COMPLETED");
        }


        // --------------------------------------------------------
        // FIND ITEM DIRECTLY
        // --------------------------------------------------------

        OrderItem orderItem =
                orderItemRepository
                        .findById(orderItemId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order item not found: "
                                        + orderItemId));


        RestaurantOrder order =
                orderItem.getOrder();


        if (order == null) {

            throw new RuntimeException(
                    "Order item is not linked to an order");
        }


        // --------------------------------------------------------
        // VERIFY ORDER NUMBER
        // --------------------------------------------------------

        if (!order.getOrderNumber()
                .equals(orderNumber)) {

            throw new RuntimeException(
                    "Order item does not belong to this order");
        }


        // --------------------------------------------------------
        // UPDATE ITEM
        // --------------------------------------------------------

        orderItem.setStatus(
                cleanStatus);

        orderItemRepository.save(
                orderItem);


        // --------------------------------------------------------
        // UPDATE OVERALL ORDER
        // --------------------------------------------------------

        updateOverallOrderStatus(order);


        return convertToResponse(order);
    }


    // ============================================================
    // UPDATE PAYMENT STATUS
    // ============================================================

    @Transactional
    public OrderResponse updatePayment(
            String orderNumber,
            String paymentStatus,
            String paymentMethod) {

        RestaurantOrder order =
                orderRepository.findByOrderNumber(orderNumber)
                        .orElseThrow(() -> new RuntimeException(
                                "Order not found"));

        String cleanStatus = paymentStatus == null
                ? ""
                : paymentStatus.trim().toUpperCase();

        String cleanMethod = paymentMethod == null
                ? "NOT_SELECTED"
                : paymentMethod.trim().toUpperCase();

        if (!cleanStatus.equals("PENDING") &&
                !cleanStatus.equals("PAID") &&
                !cleanStatus.equals("FAILED")) {
            throw new RuntimeException(
                    "Invalid payment status. Use PENDING, PAID or FAILED");
        }

        if (!cleanMethod.equals("NOT_SELECTED") &&
                !cleanMethod.equals("CASH") &&
                !cleanMethod.equals("UPI") &&
                !cleanMethod.equals("CARD")) {
            throw new RuntimeException(
                    "Invalid payment method. Use CASH, UPI or CARD");
        }

        if (cleanStatus.equals("PAID") && cleanMethod.equals("NOT_SELECTED")) {
            throw new RuntimeException(
                    "Select a payment method before marking the payment as paid");
        }

        order.setPaymentStatus(cleanStatus);
        order.setPaymentMethod(cleanMethod);
        order.setPaymentTime(
                cleanStatus.equals("PAID") ? LocalDateTime.now() : null);

        return convertToResponse(orderRepository.save(order));
    }


    // ============================================================
    // CALCULATE OVERALL ORDER STATUS
    // ============================================================

    private void updateOverallOrderStatus(
            RestaurantOrder order) {

        List<OrderItem> items =
                order.getItems();


        if (items == null ||
                items.isEmpty()) {

            order.setStatus("PENDING");

            orderRepository.save(order);

            return;
        }


        boolean allCompleted = true;

        boolean anyPreparing = false;


        for (OrderItem item :
                items) {

            String itemStatus =
                    item.getStatus();


            if ("PREPARING".equalsIgnoreCase(
                    itemStatus)) {

                anyPreparing = true;
            }


            if (!"COMPLETED".equalsIgnoreCase(
                    itemStatus)) {

                allCompleted = false;
            }
        }


        if (allCompleted) {

            order.setStatus("COMPLETED");

        } else if (anyPreparing) {

            order.setStatus("PREPARING");

        } else {

            order.setStatus("PENDING");
        }


        orderRepository.save(order);
    }


    // ============================================================
    // CONVERT ENTITY TO RESPONSE
    // ============================================================

    private OrderResponse convertToResponse(
            RestaurantOrder order) {

        OrderResponse response =
                new OrderResponse();


        // --------------------------------------------------------
        // ORDER INFORMATION
        // --------------------------------------------------------

        response.setOrderId(
                order.getOrderId());

        response.setOrderNumber(
                order.getOrderNumber());

        response.setStatus(
                order.getStatus());

        response.setCustomerName(
                order.getCustomer().getName());

        response.setPhone(
                order.getCustomer().getPhone());

        response.setOrderType(
                order.getOrderType());

        response.setRoomType(
                order.getRoomType());

        response.setFoodSubtotal(
                order.getFoodSubtotal());

        response.setRoomCharge(
                order.getRoomCharge());

        response.setGst(
                order.getGst());

        response.setTotalAmount(
                order.getTotalAmount());

        response.setBookingCharge(
                order.getBookingCharge());

        response.setReservationNumber(
                order.getReservationNumber());

        response.setPaymentStatus(
                order.getPaymentStatus());

        response.setPaymentMethod(
                order.getPaymentMethod());

        response.setPaymentTime(
                order.getPaymentTime());

        response.setOrderDate(
                order.getOrderDate());

        response.setCreatedAt(
                order.getCreatedAt());


        // --------------------------------------------------------
        // ORDER ITEMS
        // --------------------------------------------------------

        List<OrderResponse.OrderItemResponse>
                itemResponses =
                new ArrayList<>();


        if (order.getItems() != null) {

            for (OrderItem item :
                    order.getItems()) {

                Integer menuItemId =
                        null;

                String category =
                        "Main Kitchen";


                if (item.getMenuItem() != null) {

                    menuItemId =
                            item.getMenuItem()
                                    .getItemId();

                    category =
                            item.getMenuItem()
                                    .getCategory();
                }


                OrderResponse.OrderItemResponse
                        itemResponse =
                        new OrderResponse.OrderItemResponse();


                // IMPORTANT:
                // This ID is unique and is used
                // for item status updates.

                itemResponse.setOrderItemId(
                        item.getOrderItemId());


                itemResponse.setItemId(
                        menuItemId);

                itemResponse.setDishName(
                        item.getDishName());

                itemResponse.setCategory(
                        category);

                itemResponse.setPrice(
                        item.getPrice());

                itemResponse.setQuantity(
                        item.getQuantity());

                itemResponse.setSubtotal(
                        item.getSubtotal());

                itemResponse.setStatus(
                        item.getStatus());


                itemResponses.add(
                        itemResponse);
            }
        }


        response.setItems(
                itemResponses);


        return response;
    }
}