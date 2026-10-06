package restaurant_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import restaurant_management.entity.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
}