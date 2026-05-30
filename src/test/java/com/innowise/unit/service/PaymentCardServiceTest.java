package com.innowise.unit.service;

import com.innowise.dto.PaymentCardDTO;
import com.innowise.exception.BusinessException;
import com.innowise.exception.ResourceNotFoundException;
import com.innowise.mapper.PaymentCardMapper;
import com.innowise.model.PaymentCard;
import com.innowise.model.User;
import com.innowise.repository.PaymentCardRepository;
import com.innowise.repository.UserRepository;
import com.innowise.service.PaymentCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class PaymentCardServiceTest {
    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private PaymentCardService paymentCardService;

    private User user;
    private PaymentCard card;
    private PaymentCardDTO cardDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setActive(true);

        card = new PaymentCard();
        card.setId(1L);
        card.setNumber("1234567890123456");
        card.setHolder("John Doe");
        card.setExpirationDate(LocalDate.of(2028, 12, 31));
        card.setActive(true);
        card.setUser(user);

        cardDTO = new PaymentCardDTO();
        cardDTO.setId(1L);
        cardDTO.setNumber("1234567890123456");
        cardDTO.setHolder("John Doe");
        cardDTO.setExpirationDate(LocalDate.of(2028, 12, 31));
        cardDTO.setActive(true);
        cardDTO.setUserId(1L);
    }
    @Test
    void createCardTest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserAndActiveTrue(user)).thenReturn(0L);
        when(paymentCardRepository.existsByNumber(cardDTO.getNumber())).thenReturn(false);
        when(paymentCardMapper.toEntity(any(PaymentCardDTO.class))).thenReturn(card);
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(paymentCardMapper.toDto(any(PaymentCard.class))).thenReturn(cardDTO);

        PaymentCardDTO result = paymentCardService.createCard(cardDTO);

        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo("1234567890123456");
    }

    @Test
    void createCardUserNotFoundThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        cardDTO.setUserId(999L);

        assertThatThrownBy(() -> paymentCardService.createCard(cardDTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createCardMaxCardsThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserAndActiveTrue(user)).thenReturn(5L);

        assertThatThrownBy(() -> paymentCardService.createCard(cardDTO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createCardDuplicateNumberThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserAndActiveTrue(user)).thenReturn(0L);
        when(paymentCardRepository.existsByNumber(cardDTO.getNumber())).thenReturn(true);

        assertThatThrownBy(() -> paymentCardService.createCard(cardDTO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getCardByIdTest() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDto(card)).thenReturn(cardDTO);

        PaymentCardDTO result = paymentCardService.getCardById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void activateCardTest() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(paymentCardMapper.toDto(card)).thenReturn(cardDTO);

        PaymentCardDTO result = paymentCardService.activateCard(1L);

        assertThat(result).isNotNull();
        assertThat(result.getActive()).isTrue();
    }
}
