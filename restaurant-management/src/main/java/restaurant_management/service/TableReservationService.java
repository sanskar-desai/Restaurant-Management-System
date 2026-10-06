package restaurant_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurant_management.entity.RestaurantTable;
import restaurant_management.entity.TableReservation;
import restaurant_management.repository.RestaurantTableRepository;
import restaurant_management.repository.TableReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TableReservationService {

    // A table reservation blocks the table for this duration.
    private static final int RESERVATION_DURATION_MINUTES = 90;

    private final TableReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;

    public TableReservationService(
            TableReservationRepository reservationRepository,
            RestaurantTableRepository tableRepository) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
    }

    public List<RestaurantTable> getAvailableTables(
            LocalDate date, LocalTime time, Integer guests) {

        validateSearch(date, time, guests);

        List<RestaurantTable> tables = tableRepository.findAll();
        List<TableReservation> reservations =
                reservationRepository.findByReservationDate(date);

        List<RestaurantTable> available = new ArrayList<>();

        for (RestaurantTable table : tables) {
            if (!"AVAILABLE".equalsIgnoreCase(table.getStatus())) {
                continue;
            }

            if (table.getCapacity() == null || table.getCapacity() < guests) {
                continue;
            }

            if (!hasConflict(table.getTableId(), time, reservations)) {
                available.add(table);
            }
        }

        available.sort(Comparator.comparing(RestaurantTable::getTableNumber, String.CASE_INSENSITIVE_ORDER));
        return available;
    }

    @Transactional
    public TableReservation createReservation(TableReservation request) {
        validateReservation(request);

        LocalDate date = request.getReservationDate();
        LocalTime time = request.getReservationTime();

        if (date.isBefore(LocalDate.now())) {
            throw new RuntimeException("Reservation date cannot be in the past");
        }

        if (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now())) {
            throw new RuntimeException("Reservation time cannot be in the past");
        }

        RestaurantTable table = tableRepository.findById(request.getTable().getTableId())
                .orElseThrow(() -> new RuntimeException("Selected table not found"));

        if (!"AVAILABLE".equalsIgnoreCase(table.getStatus())) {
            throw new RuntimeException("Selected table is currently unavailable");
        }

        if (table.getCapacity() < request.getGuestCount()) {
            throw new RuntimeException("Selected table cannot accommodate " + request.getGuestCount() + " guests");
        }

        List<TableReservation> reservations = reservationRepository.findByReservationDate(date);
        if (hasConflict(table.getTableId(), time, reservations)) {
            throw new RuntimeException("This table is already booked for the selected time");
        }

        TableReservation reservation = new TableReservation();
        reservation.setReservationNumber(generateReservationNumber());
        reservation.setCustomerName(request.getCustomerName().trim());
        reservation.setPhone(request.getPhone().trim());
        reservation.setTable(table);
        reservation.setReservationDate(date);
        reservation.setReservationTime(time);
        reservation.setGuestCount(request.getGuestCount());
        reservation.setSpecialRequest(cleanSpecialRequest(request.getSpecialRequest()));
        reservation.setStatus("CONFIRMED");
        reservation.setCreatedAt(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    public List<TableReservation> getAllReservations() {
        List<TableReservation> reservations = reservationRepository.findAll();
        reservations.sort(Comparator
                .comparing(TableReservation::getReservationDate)
                .thenComparing(TableReservation::getReservationTime)
                .thenComparing(TableReservation::getReservationId));
        return reservations;
    }

    @Transactional
    public TableReservation updateStatus(Integer id, String status) {
        TableReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));

        String clean = cleanStatus(status);
        reservation.setStatus(clean);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void deleteReservation(Integer id) {
        if (!reservationRepository.existsById(id)) {
            throw new RuntimeException("Reservation not found: " + id);
        }
        reservationRepository.deleteById(id);
    }

    private boolean hasConflict(
            Integer tableId,
            LocalTime requestedTime,
            List<TableReservation> reservations) {

        LocalTime requestedEnd = requestedTime.plusMinutes(RESERVATION_DURATION_MINUTES);

        for (TableReservation reservation : reservations) {
            if (!reservation.getTable().getTableId().equals(tableId)) {
                continue;
            }

            if ("CANCELLED".equalsIgnoreCase(reservation.getStatus()) ||
                    "COMPLETED".equalsIgnoreCase(reservation.getStatus())) {
                continue;
            }

            LocalTime existingStart = reservation.getReservationTime();
            LocalTime existingEnd = existingStart.plusMinutes(RESERVATION_DURATION_MINUTES);

            if (requestedTime.isBefore(existingEnd) && existingStart.isBefore(requestedEnd)) {
                return true;
            }
        }

        return false;
    }

    private String generateReservationNumber() {
        long next = reservationRepository.count() + 1;
        String number;
        do {
            number = "TB-" + String.format("%05d", next++);
        } while (reservationRepository.findByReservationNumber(number).isPresent());
        return number;
    }

    private void validateSearch(LocalDate date, LocalTime time, Integer guests) {
        if (date == null) throw new RuntimeException("Date is required");
        if (time == null) throw new RuntimeException("Time is required");
        if (guests == null || guests <= 0) throw new RuntimeException("Guest count must be greater than zero");
        if (guests > 100) throw new RuntimeException("Guest count is too large");
    }

    private void validateReservation(TableReservation request) {
        if (request == null) throw new RuntimeException("Reservation details are required");
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new RuntimeException("Customer name is required");
        }
        if (request.getPhone() == null || !request.getPhone().trim().matches("[0-9]{10}")) {
            throw new RuntimeException("Enter a valid 10-digit phone number");
        }
        validateSearch(request.getReservationDate(), request.getReservationTime(), request.getGuestCount());
        if (request.getTable() == null || request.getTable().getTableId() == null) {
            throw new RuntimeException("Please select a table");
        }
    }

    private String cleanSpecialRequest(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.isEmpty() ? null : clean.substring(0, Math.min(clean.length(), 500));
    }

    private String cleanStatus(String status) {
        String clean = status == null ? "" : status.trim().toUpperCase();
        if (!clean.equals("CONFIRMED") &&
                !clean.equals("ARRIVED") &&
                !clean.equals("COMPLETED") &&
                !clean.equals("CANCELLED")) {
            throw new RuntimeException("Invalid reservation status");
        }
        return clean;
    }
}
