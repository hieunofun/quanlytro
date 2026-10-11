package com.hostel.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public final class Requests {
    private Requests() {}
    public record Login(@NotBlank String username, @NotBlank String password) { @Override public String toString() {return "Login[credentials redacted]";} }
    public record User(@NotBlank @Size(max=50) String username, @NotBlank @Size(min=12,max=72) String password, @NotBlank @Size(max=100) String fullName,
        @NotBlank @Size(max=20) String phone, @Email String email, @NotNull @Pattern(regexp="ADMIN|STAFF|TENANT") String role) { @Override public String toString() {return "User[password redacted]";} }
    public record Property(@NotBlank @Size(max=30) String code, @NotBlank @Size(max=150) String name, @NotBlank String address) {}
    public record Floor(@NotNull @Positive Long propertyId, int number, @NotBlank @Size(max=50) String name) {}
    public record Room(@NotNull @Positive Long propertyId, @NotNull @Positive Long floorId, @NotBlank @Size(max=30) String code,
        @NotNull @DecimalMin("0") BigDecimal price, @Min(1) int maxOccupants, @DecimalMin(value="0",inclusive=false) BigDecimal area) {}
    public record Tenant(Long userId, @NotBlank @Size(max=100) String fullName, @NotBlank @Size(max=20) String identityNumber, @NotBlank @Size(max=20) String phone, @Email String email) {}
    public record Contract(@NotBlank @Size(max=50) String code, @NotNull @Positive Long roomId, @NotNull @Positive Long tenantId,
        @NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull @DecimalMin(value="0",inclusive=false) BigDecimal rentalPrice,
        @NotNull @DecimalMin("0") BigDecimal deposit, @Min(1) @Max(28) int paymentDay,
        @NotNull @DecimalMin("0") BigDecimal electricIndex, @NotNull @DecimalMin("0") BigDecimal waterIndex) {}
    public record Member(@NotBlank @Size(max=100) String fullName, @Size(max=20) String identityNumber, @Size(max=20) String phone, @Size(max=50) String relationship, LocalDate effectiveFrom) {}
    public record Price(@NotNull @Positive Long propertyId, @NotNull @Positive Long serviceId, @NotNull @DecimalMin("0") BigDecimal unitPrice, @NotNull LocalDate effectiveFrom) {}
    public record Threshold(@NotNull @Positive Long propertyId, Long roomId, @NotNull @Pattern(regexp="ELECTRICITY|WATER") String meterType,
        @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("999.99") BigDecimal percentage, @Min(1) @Max(12) int months) {}
    public record Reading(@NotNull @Positive Long contractId, @NotNull @Pattern(regexp="[0-9]{4}-(0[1-9]|1[0-2])") String period,
        @NotNull @Pattern(regexp="ELECTRICITY|WATER") String meterType, @NotNull @DecimalMin("0") BigDecimal newIndex, @NotNull LocalDate readingDate) {}
    public record Invoice(@NotNull @Positive Long contractId, @NotNull @Pattern(regexp="[0-9]{4}-(0[1-9]|1[0-2])") String period, LocalDate issueDate, LocalDate dueDate) {}
    public record Payment(@NotBlank @Size(max=50) String code, @NotNull @DecimalMin(value="0",inclusive=false) BigDecimal amount,
        @NotNull @Pattern(regexp="CASH|BANK_TRANSFER_MANUAL|OTHER") String method, String notes) {}
    public record Alert(@NotNull @Pattern(regexp="CONFIRMED|DISMISSED") String status, String note) {}
    public record Checkout(@NotNull LocalDate date, @NotNull @DecimalMin("0") BigDecimal electricIndex, @NotNull @DecimalMin("0") BigDecimal waterIndex,
        @NotNull @DecimalMin("0") BigDecimal damage, String notes) {}
}
