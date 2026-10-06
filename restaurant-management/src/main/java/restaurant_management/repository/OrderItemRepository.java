package restaurant_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import restaurant_management.entity.OrderItem;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Integer> {

}