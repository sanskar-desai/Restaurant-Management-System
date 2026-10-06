package restaurant_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import restaurant_management.entity.RestaurantTable;

import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {

    Optional<RestaurantTable> findByTableNumberIgnoreCase(String tableNumber);

    boolean existsByTableNumberIgnoreCase(String tableNumber);
}
