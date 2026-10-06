package restaurant_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import restaurant_management.entity.TableReservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TableReservationRepository extends JpaRepository<TableReservation, Integer> {

    Optional<TableReservation> findByReservationNumber(String reservationNumber);

    List<TableReservation> findByReservationDate(LocalDate reservationDate);
}
