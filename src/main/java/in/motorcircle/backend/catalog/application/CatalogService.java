package in.motorcircle.backend.catalog.application;

import in.motorcircle.backend.catalog.api.CarDetailDto;
import in.motorcircle.backend.catalog.api.CarSummaryDto;
import in.motorcircle.backend.catalog.api.CarVariantSummaryDto;
import in.motorcircle.backend.catalog.domain.BodyType;
import in.motorcircle.backend.catalog.domain.Car;
import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.catalog.domain.FuelType;
import in.motorcircle.backend.catalog.domain.Transmission;
import in.motorcircle.backend.catalog.infrastructure.CarRepository;
import in.motorcircle.backend.catalog.infrastructure.CarVariantRepository;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CatalogService {

    private final CarRepository carRepository;
    private final CarVariantRepository carVariantRepository;

    public CatalogService(CarRepository carRepository, CarVariantRepository carVariantRepository) {
        this.carRepository = carRepository;
        this.carVariantRepository = carVariantRepository;
    }

    @Transactional(readOnly = true)
    public List<CarSummaryDto> getCars(String brand, String bodyType, BigDecimal minPrice, BigDecimal maxPrice) {
        BodyType typeEnum = null;
        if (bodyType != null && !bodyType.isBlank()) {
            try {
                typeEnum = BodyType.valueOf(bodyType.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<Car> cars = carRepository.searchCars(brand, typeEnum, minPrice, maxPrice);

        return cars.stream().map(car -> {
            List<CarVariant> variants = carVariantRepository.findByCarIdAndIsActiveTrue(car.getId());
            return new CarSummaryDto(
                    car.getId(),
                    car.getBrand(),
                    car.getModel(),
                    car.getBodyType().name(),
                    car.getImageUrl(),
                    car.getMinPrice(),
                    car.getMaxPrice(),
                    variants.size()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public CarDetailDto getCarById(String id) {
        Car car = carRepository.findById(id)
                .filter(Car::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Car", "id", id));

        List<CarVariant> variants = carVariantRepository.findByCarIdAndIsActiveTrue(car.getId());
        List<CarVariantSummaryDto> variantDtos = variants.stream()
                .map(v -> new CarVariantSummaryDto(
                        v.getId(),
                        v.getName(),
                        v.getFuelType().name(),
                        v.getTransmission().name(),
                        v.getExShowroomPrice(),
                        v.getKeyFeatures()
                )).toList();

        return new CarDetailDto(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getBodyType().name(),
                car.getImageUrl(),
                car.getMinPrice(),
                car.getMaxPrice(),
                variantDtos
        );
    }

    @Transactional
    public CarDetailDto createCar(in.motorcircle.backend.catalog.api.CreateCarRequest request) {
        Car car = carRepository.findByBrandIgnoreCaseAndModelIgnoreCase(request.brand(), request.model())
                .orElseGet(() -> {
                    BodyType bodyType = BodyType.SUV;
                    if (request.bodyType() != null) {
                        try {
                            bodyType = BodyType.valueOf(request.bodyType().toUpperCase());
                        } catch (Exception ignored) {}
                    }

                    Car newCar = new Car(
                            java.util.UUID.randomUUID().toString(),
                            request.brand(),
                            request.model(),
                            bodyType,
                            request.imageUrl() != null ? request.imageUrl() : "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80",
                            request.minPrice(),
                            request.maxPrice()
                    );
                    return carRepository.save(newCar);
                });

        if (request.variants() != null && !request.variants().isEmpty()) {
            for (var vReq : request.variants()) {
                if (!carVariantRepository.existsByCarIdAndNameIgnoreCase(car.getId(), vReq.name())) {
                    FuelType fuel = FuelType.PETROL;
                    if (vReq.fuelType() != null) {
                        try {
                            fuel = FuelType.valueOf(vReq.fuelType().toUpperCase());
                        } catch (Exception ignored) {}
                    }

                    Transmission trans = Transmission.MANUAL;
                    if (vReq.transmission() != null) {
                        try {
                            trans = Transmission.valueOf(vReq.transmission().toUpperCase());
                        } catch (Exception ignored) {}
                    }

                    CarVariant variant = new CarVariant(
                            java.util.UUID.randomUUID().toString(),
                            car,
                            vReq.name(),
                            fuel,
                            trans,
                            vReq.exShowroomPrice(),
                            vReq.keyFeatures() != null ? vReq.keyFeatures() : "Standard Equipment"
                    );
                    carVariantRepository.save(variant);
                }
            }
        }

        return getCarById(car.getId());
    }
}
