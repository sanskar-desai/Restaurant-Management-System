package restaurant_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import restaurant_management.entity.RestaurantTable;
import restaurant_management.service.RestaurantTableService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class RestaurantTableController {

    private final RestaurantTableService tableService;

    public RestaurantTableController(RestaurantTableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    public ResponseEntity<List<RestaurantTable>> getAllTables() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    @PostMapping
    public ResponseEntity<RestaurantTable> createTable(
            @RequestBody RestaurantTable table) {
        return ResponseEntity.ok(tableService.createTable(table));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantTable> updateTable(
            @PathVariable Integer id,
            @RequestBody RestaurantTable table) {
        return ResponseEntity.ok(tableService.updateTable(id, table));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RestaurantTable> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(tableService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(
            @PathVariable Integer id) {

        tableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}
