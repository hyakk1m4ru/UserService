package com.innowise.repository;

import com.innowise.model.PaymentCard;
import com.innowise.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {

    List<PaymentCard> findByUser(User user);

    List<PaymentCard> findByUserAndActiveTrue(User user);

    Page<PaymentCard> findByUser(User user, Pageable pageable);

    Optional<PaymentCard> findByNumber(String number);

    long countByUserAndActiveTrue(User user);

    boolean existsByNumber(String number);

    @Query(value = "SELECT * FROM payment_cards WHERE user_id = :userId",
            nativeQuery = true)
    List<PaymentCard> findCardsByUserIdNative(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE payment_cards SET active = :active WHERE id = :cardId",
            nativeQuery = true)
    int updateCardActiveStatusNative(@Param("cardId") Long cardId,
                                     @Param("active") Boolean active);

    @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId",
            nativeQuery = true)
    int countCardsByUserIdNative(@Param("userId") Long userId);

    @Query("SELECT pc FROM PaymentCard pc WHERE pc.user.id = :userId")
    List<PaymentCard> findCardsByUserIdJPQL(@Param("userId") Long userId);

    @Query("SELECT pc FROM PaymentCard pc WHERE pc.user = :user AND pc.active = true")
    List<PaymentCard> findActiveCardsByUserJPQL(@Param("user") User user);

    @Query("SELECT pc FROM PaymentCard pc WHERE pc.expirationDate < CURRENT_DATE")
    List<PaymentCard> findExpiredCardsJPQL();

    @Query("SELECT COUNT(pc) FROM PaymentCard pc WHERE pc.user.id = :userId")
    long countCardsByUserIdJPQL(@Param("userId") Long userId);
}