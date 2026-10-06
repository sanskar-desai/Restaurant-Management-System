package restaurant_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurant_management.entity.RestaurantTable;
import restaurant_management.repository.RestaurantTableRepository;

import java.util.List;

@Service
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;

    public RestaurantTableService(RestaurantTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    public List<RestaurantTable> getAllTables() {
        return tableRepository.findAll();
    }

    @Transactional
    public RestaurantTable createTable(RestaurantTable table) {
        validateTable(table);

        String tableNumber = table.getTableNumber().trim().toUpperCase();

        if (tableRepository.existsByTableNumberIgnoreCase(tableNumber)) {
            throw new RuntimeException("Table number already exists: " + tableNumber);
        }

        table.setTableNumber(tableNumber);
        table.setSection(table.getSection().trim());
        table.setStatus(cleanStatus(table.getStatus()));

        return tableRepository.save(table);
    }

    @Transactional
    public RestaurantTable updateTable(Integer id, RestaurantTable updatedTable) {
        validateTable(updatedTable);

        RestaurantTable existing = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));

        String tableNumber = updatedTable.getTableNumber().trim().toUpperCase();

        tableRepository.findByTableNumberIgnoreCase(tableNumber).ifPresent(found -> {
            if (!found.getTableId().equals(id)) {
                throw new RuntimeException("Table number already exists: " + tableNumber);
            }
        });

        existing.setTableNumber(tableNumber);
        existing.setCapacity(updatedTable.getCapacity());
        existing.setSection(updatedTable.getSection().trim());
        existing.setStatus(cleanStatus(updatedTable.getStatus()));

        return tableRepository.save(existing);
    }

    @Transactional
    public RestaurantTable updateStatus(Integer id, String status) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));

        table.setStatus(cleanStatus(status));
        return tableRepository.save(table);
    }

    @Transactional
    public void deleteTable(Integer id) {
        if (!tableRepository.existsById(id)) {
            throw new RuntimeException("Table not found: " + id);
        }

        tableRepository.deleteById(id);
    }

    private void validateTable(RestaurantTable table) {
        if (table == null) {
            throw new RuntimeException("Table details are required");
        }

        if (table.getTableNumber() == null || table.getTableNumber().trim().isEmpty()) {
            throw new RuntimeException("Table number is required");
        }

        if (table.getCapacity() == null || table.getCapacity() <= 0) {
            throw new RuntimeException("Capacity must be greater than zero");
        }

        if (table.getSection() == null || table.getSection().trim().isEmpty()) {
            throw new RuntimeException("Section is required");
        }
    }

    private String cleanStatus(String status) {
        String clean = status == null ? "AVAILABLE" : status.trim().toUpperCase();

        if (!clean.equals("AVAILABLE") &&
                !clean.equals("OCCUPIED") &&
                !clean.equals("RESERVED")) {
            throw new RuntimeException(
                    "Invalid table status. Use AVAILABLE, OCCUPIED or RESERVED");
        }

        return clean;
    }
}
