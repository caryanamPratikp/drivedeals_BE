package in.motorcircle.backend.catalog.infrastructure;

import in.motorcircle.backend.catalog.domain.BodyType;
import in.motorcircle.backend.catalog.domain.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, String> {

    List<Car> findByIsActiveTrue();

    java.util.Optional<Car> findByBrandIgnoreCaseAndModelIgnoreCase(String brand, String model);

    @Query("SELECT c FROM Car c WHERE c.isActive = true " +
           "AND (:brand IS NULL OR LOWER(c.brand) = LOWER(:brand)) " +
           "AND (:bodyType IS NULL OR c.bodyType = :bodyType) " +
           "AND (:minPrice IS NULL OR c.maxPrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR c.minPrice <= :maxPrice) " +
           "ORDER BY c.minPrice ASC")
    List<Car> searchCars(
            @Param("brand") String brand,
            @Param("bodyType") BodyType bodyType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
