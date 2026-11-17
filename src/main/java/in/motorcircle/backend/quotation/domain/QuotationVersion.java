package in.motorcircle.backend.quotation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "quotation_versions")
public class QuotationVersion {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "ex_showroom_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal exShowroomPrice;

    @Column(name = "registration_charges", precision = 12, scale = 2, nullable = false)
    private BigDecimal registrationCharges;

    @Column(name = "insurance_charges", precision = 12, scale = 2, nullable = false)
    private BigDecimal insuranceCharges;

    @Column(name = "accessories_charges", precision = 12, scale = 2, nullable = false)
    private BigDecimal accessoriesCharges = BigDecimal.ZERO;

    @Column(name = "other_charges", precision = 12, scale = 2, nullable = false)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "group_discount", precision = 12, scale = 2, nullable = false)
    private BigDecimal groupDiscount = BigDecimal.ZERO;

    @Column(name = "corporate_discount", precision = 12, scale = 2, nullable = false)
    private BigDecimal corporateDiscount = BigDecimal.ZERO;

    @Column(name = "exchange_bonus", precision = 12, scale = 2, nullable = false)
    private BigDecimal exchangeBonus = BigDecimal.ZERO;

    @Column(name = "total_on_road_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalOnRoadPrice;

    @Column(name = "accessory_package", columnDefinition = "TEXT")
    private String accessoryPackage;

    @Column(name = "warranty_years", nullable = false)
    private int warrantyYears = 3;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "delivery_timeline_weeks", nullable = false)
    private int deliveryTimelineWeeks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public QuotationVersion() {
    }

    public QuotationVersion(
            String id,
            Quotation quotation,
            int versionNumber,
            BigDecimal exShowroomPrice,
            BigDecimal registrationCharges,
            BigDecimal insuranceCharges,
            BigDecimal accessoriesCharges,
            BigDecimal otherCharges,
            BigDecimal groupDiscount,
            BigDecimal corporateDiscount,
            BigDecimal exchangeBonus,
            BigDecimal totalOnRoadPrice,
            String accessoryPackage,
            int warrantyYears,
            Instant validUntil,
            int deliveryTimelineWeeks
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.quotation = quotation;
        this.versionNumber = versionNumber;
        this.exShowroomPrice = exShowroomPrice;
        this.registrationCharges = registrationCharges;
        this.insuranceCharges = insuranceCharges;
        this.accessoriesCharges = accessoriesCharges != null ? accessoriesCharges : BigDecimal.ZERO;
        this.otherCharges = otherCharges != null ? otherCharges : BigDecimal.ZERO;
        this.groupDiscount = groupDiscount != null ? groupDiscount : BigDecimal.ZERO;
        this.corporateDiscount = corporateDiscount != null ? corporateDiscount : BigDecimal.ZERO;
        this.exchangeBonus = exchangeBonus != null ? exchangeBonus : BigDecimal.ZERO;
        this.totalOnRoadPrice = totalOnRoadPrice;
        this.accessoryPackage = accessoryPackage;
        this.warrantyYears = warrantyYears;
        this.validUntil = validUntil;
        this.deliveryTimelineWeeks = deliveryTimelineWeeks;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Quotation getQuotation() {
        return quotation;
    }

    public void setQuotation(Quotation quotation) {
        this.quotation = quotation;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public BigDecimal getExShowroomPrice() {
        return exShowroomPrice;
    }

    public void setExShowroomPrice(BigDecimal exShowroomPrice) {
        this.exShowroomPrice = exShowroomPrice;
    }

    public BigDecimal getRegistrationCharges() {
        return registrationCharges;
    }

    public void setRegistrationCharges(BigDecimal registrationCharges) {
        this.registrationCharges = registrationCharges;
    }

    public BigDecimal getInsuranceCharges() {
        return insuranceCharges;
    }

    public void setInsuranceCharges(BigDecimal insuranceCharges) {
        this.insuranceCharges = insuranceCharges;
    }

    public BigDecimal getAccessoriesCharges() {
        return accessoriesCharges;
    }

    public void setAccessoriesCharges(BigDecimal accessoriesCharges) {
        this.accessoriesCharges = accessoriesCharges;
    }

    public BigDecimal getOtherCharges() {
        return otherCharges;
    }

    public void setOtherCharges(BigDecimal otherCharges) {
        this.otherCharges = otherCharges;
    }

    public BigDecimal getGroupDiscount() {
        return groupDiscount;
    }

    public void setGroupDiscount(BigDecimal groupDiscount) {
        this.groupDiscount = groupDiscount;
    }

    public BigDecimal getCorporateDiscount() {
        return corporateDiscount;
    }

    public void setCorporateDiscount(BigDecimal corporateDiscount) {
        this.corporateDiscount = corporateDiscount;
    }

    public BigDecimal getExchangeBonus() {
        return exchangeBonus;
    }

    public void setExchangeBonus(BigDecimal exchangeBonus) {
        this.exchangeBonus = exchangeBonus;
    }

    public BigDecimal getTotalOnRoadPrice() {
        return totalOnRoadPrice;
    }

    public void setTotalOnRoadPrice(BigDecimal totalOnRoadPrice) {
        this.totalOnRoadPrice = totalOnRoadPrice;
    }

    public String getAccessoryPackage() {
        return accessoryPackage;
    }

    public void setAccessoryPackage(String accessoryPackage) {
        this.accessoryPackage = accessoryPackage;
    }

    public int getWarrantyYears() {
        return warrantyYears;
    }

    public void setWarrantyYears(int warrantyYears) {
        this.warrantyYears = warrantyYears;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public int getDeliveryTimelineWeeks() {
        return deliveryTimelineWeeks;
    }

    public void setDeliveryTimelineWeeks(int deliveryTimelineWeeks) {
        this.deliveryTimelineWeeks = deliveryTimelineWeeks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuotationVersion that = (QuotationVersion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
