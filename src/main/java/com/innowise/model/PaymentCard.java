package com.innowise.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "payment_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@NamedQueries({
        @NamedQuery(
                name = "PaymentCard.findByUserId",
                query = "SELECT pc FROM PaymentCard pc WHERE pc.user.id = :userId"
        ),
        @NamedQuery(
                name = "PaymentCard.findActiveCardsByUserId",
                query = "SELECT pc FROM PaymentCard pc WHERE pc.user.id = :userId AND pc.active = true"
        )
})
public class PaymentCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 16)
    private String number;

    @Column(nullable = false, length = 100)
    private String holder;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false)
    private Boolean active = true;
}