package com.innowise.service;



import com.innowise.dto.PageResponse;
import com.innowise.dto.PaymentCardDTO;
import com.innowise.dto.UserDTO;
import com.innowise.exception.BusinessException;
import com.innowise.exception.ResourceNotFoundException;
import com.innowise.mapper.PaymentCardMapper;
import com.innowise.model.PaymentCard;
import com.innowise.model.User;
import com.innowise.repository.PaymentCardRepository;
import com.innowise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    public PaymentCardDTO createCard(PaymentCardDTO cardDTO){
        User user = userRepository.findById(cardDTO.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User", cardDTO.getUserId()));
        long activeCardsCount = paymentCardRepository.countByUserAndActiveTrue(user);
        if(activeCardsCount >= 5){
            throw new BusinessException("User cant have >= 5 active cards");
        }
        if(paymentCardRepository.existsByNumber(cardDTO.getNumber())){
            throw new BusinessException("Card with number " + cardDTO.getNumber() + " already exists");
        }

        PaymentCard card = paymentCardMapper.toEntity(cardDTO);
        card.setUser(user);
        card.setActive(true);

        PaymentCard savedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(savedCard);
    }

    public PaymentCardDTO getCardById(Long id){
        PaymentCard card = paymentCardRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Payment card", id));
        return paymentCardMapper.toDto(card);
    }

    public PageResponse<PaymentCardDTO> getAllCards(int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentCard> cardPage = paymentCardRepository.findAll(pageable);
        List<PaymentCardDTO> cardDTOS = cardPage.getContent().stream().map(paymentCardMapper::toDto).toList();
        return new PageResponse<>(cardDTOS, cardPage.getNumber(),cardPage.getSize(),cardPage.getTotalElements());
    }
    public List<PaymentCardDTO> getCardsByUserId(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return paymentCardRepository.findByUser(user).stream().map(paymentCardMapper::toDto).toList();
    }

    @Transactional
    public PaymentCardDTO updateCard(Long id, PaymentCardDTO cardDTO){
        PaymentCard card = paymentCardRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Payment card ", id));

        if(!card.getNumber().equals(cardDTO.getNumber()) && paymentCardRepository.existsByNumber(cardDTO.getNumber()) ){
            throw new BusinessException("Card number "+ cardDTO.getNumber()+" already exists");
        }

        paymentCardMapper.updateEntityFromDto(cardDTO, card);
        PaymentCard updatedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(updatedCard);
    }

    @Transactional
    public void activateCard(Long id){
        updateCardActiveStatus(id, true);
    }
    @Transactional
    public void deactivateCard(Long id){
        updateCardActiveStatus(id, false);
    }
    private void updateCardActiveStatus(Long id, Boolean active){
        PaymentCard card = paymentCardRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Payment card ", id));
        card.setActive(active);
        paymentCardRepository.save(card);
    }
}
