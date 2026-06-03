package com.innowise.dto;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

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