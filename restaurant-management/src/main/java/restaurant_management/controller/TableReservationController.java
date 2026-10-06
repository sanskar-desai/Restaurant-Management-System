package restaurant_management.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import restaurant_management.entity.RestaurantTable;
import restaurant_management.entity.TableReservation;
import restaurant_management.service.TableReservationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/table-reservations")
@CrossOrigin(origins = "*")
public class TableReservationController {

    private final TableReservationService reservationService;

    public TableReservationController(TableReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/available-tables")
    public ResponseEntity<List<RestaurantTable>> getAvailableTables(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime time,
            @RequestParam Integer guests) {

        return ResponseEntity.ok(
                reservationService.getAvailableTables(date, time, guests));
    }

    @PostMapping
    public ResponseEntity<TableReservation> createReservation(
            @RequestBody TableReservation request) {

        return ResponseEntity.ok(
                reservationService.createReservation(request));
    }

    @GetMapping
    public ResponseEntity<List<TableReservation>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TableReservation> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(reservationService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Integer id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
