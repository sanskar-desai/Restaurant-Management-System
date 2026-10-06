package restaurant_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import restaurant_management.entity.Customer;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByPhone(String phone);
}