package restaurant_management.controller;

import org.springframework.web.bind.annotation.*;
import restaurant_management.entity.MenuItem;
import restaurant_management.repository.MenuItemRepository;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuItemController {

    private final MenuItemRepository menuItemRepository;

    public MenuItemController(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    // GET ALL MENU ITEMS
    @GetMapping
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    // GET ONE MENU ITEM
    @GetMapping("/{id}")
    public MenuItem getMenuItem(@PathVariable Integer id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
    }

    // ADD MENU ITEM
    @PostMapping
    public MenuItem addMenuItem(@RequestBody MenuItem menuItem) {
        return menuItemRepository.save(menuItem);
    }

    // UPDATE MENU ITEM
    @PutMapping("/{id}")
    public MenuItem updateMenuItem(
            @PathVariable Integer id,
            @RequestBody MenuItem updatedItem) {

        MenuItem existingItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        existingItem.setDishName(updatedItem.getDishName());
        existingItem.setPrice(updatedItem.getPrice());
        existingItem.setCategory(updatedItem.getCategory());

        // Keep the current availability unless a value was explicitly sent.
        if (updatedItem.getAvailable() != null) {
            existingItem.setAvailable(updatedItem.getAvailable());
        }

        return menuItemRepository.save(existingItem);
    }

    // UPDATE ITEM AVAILABILITY
    @PatchMapping("/{id}/availability")
    public MenuItem updateAvailability(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, Boolean> request) {

        MenuItem existingItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        Boolean available = request.get("available");

        if (available == null) {
            throw new RuntimeException("Availability value is required");
        }

        existingItem.setAvailable(available);
        return menuItemRepository.save(existingItem);
    }

    // DELETE MENU ITEM
    @DeleteMapping("/{id}")
    public void deleteMenuItem(@PathVariable Integer id) {

        MenuItem existingItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        menuItemRepository.delete(existingItem);
    }
}