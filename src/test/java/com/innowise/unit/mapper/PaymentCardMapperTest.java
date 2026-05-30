package com.innowise.unit.mapper;

import com.innowise.dto.PaymentCardDTO;
import com.innowise.mapper.PaymentCardMapper;
import com.innowise.model.PaymentCard;
import com.innowise.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCardMapperTest {

    private PaymentCardMapper paymentCardMapper;

    @BeforeEach
    void setUp() {
        paymentCardMapper = Mappers.getMapper(PaymentCardMapper.class);
    }

    @Test
    void mapEntityToDto() {
        User user = new User();
        user.setId(10L);
        user.setName("John");
        user.setSurname("Doe");

        PaymentCard card = new PaymentCard();
        card.setId(1L);
        card.setNumber("1234567890123456");
        card.setHolder("John Doe");
        card.setExpirationDate(LocalDate.of(2028, 12, 31));
        card.setActive(true);
        card.setUser(user);

        PaymentCardDTO dto = paymentCardMapper.toDto(card);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNumber()).isEqualTo("1234567890123456");
        assertThat(dto.getHolder()).isEqualTo("John Doe");
        assertThat(dto.getExpirationDate()).isEqualTo(LocalDate.of(2028, 12, 31));
        assertThat(dto.getActive()).isTrue();
        assertThat(dto.getUserId()).isEqualTo(10L);
    }

    @Test
    void toDtoWhenUserIsNullSetUserIdNull() {
        PaymentCard card = new PaymentCard();
        card.setId(1L);
        card.setNumber("1234567890123456");
        card.setHolder("John Doe");
        card.setExpirationDate(LocalDate.of(2028, 12, 31));
        card.setActive(true);
        card.setUser(null);

        PaymentCardDTO dto = paymentCardMapper.toDto(card);

        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isNull();
    }

    @Test
    void mapDtoToEntity() {
        PaymentCardDTO dto = new PaymentCardDTO();
        dto.setId(1L);
        dto.setNumber("1234567890123456");
        dto.setHolder("John Doe");
        dto.setExpirationDate(LocalDate.of(2028, 12, 31));
        dto.setActive(true);
        dto.setUserId(10L);

        PaymentCard card = paymentCardMapper.toEntity(dto);

        assertThat(card).isNotNull();
        assertThat(card.getId()).isNull();
        assertThat(card.getNumber()).isEqualTo("1234567890123456");
        assertThat(card.getHolder()).isEqualTo("John Doe");
        assertThat(card.getExpirationDate()).isEqualTo(LocalDate.of(2028, 12, 31));
        assertThat(card.getActive()).isTrue();
        assertThat(card.getUser()).isNotNull();
        assertThat(card.getUser().getId()).isEqualTo(10L);
        assertThat(card.getUser().getName()).isNull();
        assertThat(card.getUser().getSurname()).isNull();
    }

    @Test
    void toEntityWhenUserIdIsNullSetUserNull() {
        PaymentCardDTO dto = new PaymentCardDTO();
        dto.setNumber("1234567890123456");
        dto.setHolder("John Doe");
        dto.setExpirationDate(LocalDate.of(2028, 12, 31));
        dto.setActive(true);
        dto.setUserId(null);

        PaymentCard card = paymentCardMapper.toEntity(dto);

        assertThat(card).isNotNull();
        assertThat(card.getUser()).isNull();
    }

    @Test
    void updateEntityFromDtoUpdateOnlyAllowedFields() {
        User user = new User();
        user.setId(10L);

        PaymentCard existingCard = new PaymentCard();
        existingCard.setId(1L);
        existingCard.setNumber("1234567890123456");
        existingCard.setHolder("Old Name");
        existingCard.setExpirationDate(LocalDate.of(2027, 12, 31));
        existingCard.setActive(true);
        existingCard.setUser(user);

        PaymentCardDTO updateDto = new PaymentCardDTO();
        updateDto.setNumber("9999999999999999");
        updateDto.setHolder("New Name");
        updateDto.setExpirationDate(LocalDate.of(2029, 12, 31));
        updateDto.setActive(false);
        updateDto.setUserId(20L);
        updateDto.setId(100L);

        paymentCardMapper.updateEntityFromDto(updateDto, existingCard);

        assertThat(existingCard.getId()).isEqualTo(1L);

        assertThat(existingCard.getNumber()).isEqualTo("9999999999999999");
        assertThat(existingCard.getHolder()).isEqualTo("New Name");
        assertThat(existingCard.getExpirationDate()).isEqualTo(LocalDate.of(2029, 12, 31));
        assertThat(existingCard.getActive()).isTrue();
        assertThat(existingCard.getUser().getId()).isEqualTo(10L);
    }

    @Test
    void updateEntityFromDtoWhenDtoFieldsNullNotUpdateValues() {
        PaymentCard existingCard = new PaymentCard();
        existingCard.setId(1L);
        existingCard.setNumber("1234567890123456");
        existingCard.setHolder("Old Name");
        existingCard.setExpirationDate(LocalDate.of(2027, 12, 31));
        existingCard.setActive(true);

        PaymentCardDTO updateDto = new PaymentCardDTO();

        paymentCardMapper.updateEntityFromDto(updateDto, existingCard);

        assertThat(existingCard.getNumber()).isEqualTo("1234567890123456");
        assertThat(existingCard.getHolder()).isEqualTo("Old Name");
        assertThat(existingCard.getExpirationDate()).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(existingCard.getActive()).isTrue();
    }

    @Test
    void createUserWithOnlyIdTest() {
        Long userId = 10L;

        User user = paymentCardMapper.userIdToUser(userId);

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(10L);
        assertThat(user.getName()).isNull();
        assertThat(user.getSurname()).isNull();
        assertThat(user.getEmail()).isNull();
    }

    @Test
    void userIdToUserWhenUserIdIsNullShouldReturnNull() {
        User user = paymentCardMapper.userIdToUser(null);

        assertThat(user).isNull();
    }

    @Test
    void toEntityAndToDtoConsistenceTest() {
        PaymentCardDTO originalDto = new PaymentCardDTO();
        originalDto.setNumber("1234567890123456");
        originalDto.setHolder("John Doe");
        originalDto.setExpirationDate(LocalDate.of(2028, 12, 31));
        originalDto.setActive(true);
        originalDto.setUserId(10L);

        PaymentCard card = paymentCardMapper.toEntity(originalDto);
        PaymentCardDTO resultDto = paymentCardMapper.toDto(card);

        assertThat(resultDto.getNumber()).isEqualTo(originalDto.getNumber());
        assertThat(resultDto.getHolder()).isEqualTo(originalDto.getHolder());
        assertThat(resultDto.getExpirationDate()).isEqualTo(originalDto.getExpirationDate());
        assertThat(resultDto.getActive()).isEqualTo(originalDto.getActive());
        assertThat(resultDto.getUserId()).isEqualTo(originalDto.getUserId());
    }
}