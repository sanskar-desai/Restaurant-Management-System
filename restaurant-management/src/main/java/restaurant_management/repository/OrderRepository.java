package restaurant_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import restaurant_management.entity.RestaurantOrder;

import java.time.LocalDate;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<RestaurantOrder, Integer> {

    // Used by admin/payment/status functions
    Optional<RestaurantOrder> findByOrderNumber(String orderNumber);

    // Used by customer order tracking - today's orders only
    Optional<RestaurantOrder> findByOrderNumberAndOrderDate(
            String orderNumber,
            LocalDate orderDate
    );

    boolean existsByReservationNumber(String reservationNumber);

    Optional<RestaurantOrder> findTopByOrderDateOrderByDailySequenceDesc(
            LocalDate orderDate
    );
}