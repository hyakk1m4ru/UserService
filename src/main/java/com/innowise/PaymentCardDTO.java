package com.innowise;


import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
public class PaymentCardDTO {
    private Long id;

    @NotBlank
    @Pattern(regexp = "\\d{16}")
    private String number;

    @NotBlank
    private String holder;

    @NotNull
    @Future
    private LocalDate expirationDate;

    private Boolean active;
    private Long userId;
}