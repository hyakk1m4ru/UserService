package com.innowise.service;


import com.innowise.dto.PageResponse;
import com.innowise.dto.PaymentCardDTO;
import com.innowise.exception.BusinessException;
import com.innowise.exception.ResourceNotFoundException;
import com.innowise.mapper.PaymentCardMapper;
import com.innowise.model.PaymentCard;
import com.innowise.model.User;
import com.innowise.repository.PaymentCardRepository;
import com.innowise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    @CacheEvict(value = "user", key = "#cardDTO.userId")
    public PaymentCardDTO createCard(PaymentCardDTO cardDTO) {
        User user = userRepository.findById(cardDTO.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User", cardDTO.getUserId()));
        long activeCardsCount = paymentCardRepository.countByUserAndActiveTrue(user);
        if (activeCardsCount >= 5) {
            throw new BusinessException("User cant have >= 5 active cards");
        }
        if (paymentCardRepository.existsByNumber(cardDTO.getNumber())) {
            throw new BusinessException("Card with number " + cardDTO.getNumber() + " already exists");
        }

        PaymentCard card = paymentCardMapper.toEntity(cardDTO);
        card.setUser(user);
        card.setActive(true);

        PaymentCard savedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(savedCard);
    }
    @Cacheable(value = "card", key = "#id")
    public PaymentCardDTO getCardById(Long id) {
        PaymentCard card = paymentCardRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment card", id));
        return paymentCardMapper.toDto(card);
    }

    public PageResponse<PaymentCardDTO> getAllCards(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentCard> cardPage = paymentCardRepository.findAll(pageable);
        List<PaymentCardDTO> cardDTOS = cardPage.getContent().stream().map(paymentCardMapper::toDto).toList();
        return new PageResponse<>(cardDTOS, cardPage.getNumber(), cardPage.getSize(), cardPage.getTotalElements());
    }

    @Cacheable(value = "card", key = "#userId")
    public List<PaymentCardDTO> getCardsByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return paymentCardRepository.findByUser(user).stream().map(paymentCardMapper::toDto).toList();
    }

    @Transactional
    @CachePut(value = "card", key = "#id")
    @CacheEvict(value = "userCards", key = "#result.userId")
    public PaymentCardDTO updateCard(Long id, PaymentCardDTO cardDTO) {
        PaymentCard card = paymentCardRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment card ", id));

        if (!card.getNumber().equals(cardDTO.getNumber()) && paymentCardRepository.existsByNumber(cardDTO.getNumber())) {
            throw new BusinessException("Card number " + cardDTO.getNumber() + " already exists");
        }

        paymentCardMapper.updateEntityFromDto(cardDTO, card);
        PaymentCard updatedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(updatedCard);
    }

    @Transactional
    @CachePut(value = "card", key = "#id")
    @CacheEvict(value = "userCards", key = "#result.userId")
    public PaymentCardDTO activateCard(Long id) {
        return updateCardActiveStatus(id, true);
    }

    @Transactional
    @CachePut(value = "card", key = "#result.userId")
    @CacheEvict(value = "userCards", key = "#result.userId")
    public PaymentCardDTO deactivateCard(Long id) {
        return updateCardActiveStatus(id, false);
    }

    private PaymentCardDTO updateCardActiveStatus(Long id, Boolean active) {
        PaymentCard card = paymentCardRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment card ", id));
        card.setActive(active);
        PaymentCard savedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(savedCard);
    }
}
