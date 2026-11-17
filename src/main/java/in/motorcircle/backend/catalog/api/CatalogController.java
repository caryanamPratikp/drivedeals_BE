package in.motorcircle.backend.catalog.api;

import in.motorcircle.backend.catalog.application.CatalogService;
import in.motorcircle.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/catalogue")
@Tag(name = "Vehicle Catalogue", description = "Endpoints for vehicle models, variants, and pricing benchmarks")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/cars")
    @Operation(summary = "Browse Vehicles", description = "Filter active car models by brand, body type, and price range")
    public ResponseEntity<ApiResponse<List<CarSummaryDto>>> getCars(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String bodyType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        List<CarSummaryDto> cars = catalogService.getCars(brand, bodyType, minPrice, maxPrice);
        return ResponseEntity.ok(ApiResponse.success(cars));
    }

    @GetMapping("/cars/{id}")
    @Operation(summary = "Get Vehicle Details", description = "Returns complete specifications, price range, and available trims for a model")
    public ResponseEntity<ApiResponse<CarDetailDto>> getCarById(@PathVariable String id) {
        CarDetailDto car = catalogService.getCarById(id);
        return ResponseEntity.ok(ApiResponse.success(car));
    }

    @PostMapping("/cars")
    @Operation(summary = "Add New Vehicle", description = "Admin/Manager endpoint to register new vehicle model with trim variants")
    public ResponseEntity<ApiResponse<CarDetailDto>> createCar(@jakarta.validation.Valid @RequestBody CreateCarRequest request) {
        CarDetailDto created = catalogService.createCar(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Vehicle registered successfully"));
    }
}
