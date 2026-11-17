package in.motorcircle.backend.catalog.infrastructure;

import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.catalog.domain.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarVariantRepository extends JpaRepository<CarVariant, String> {
    List<CarVariant> findByCarIdAndIsActiveTrue(String carId);
    List<CarVariant> findByCarIdAndFuelTypeAndIsActiveTrue(String carId, FuelType fuelType);
    boolean existsByCarIdAndNameIgnoreCase(String carId, String name);
}
