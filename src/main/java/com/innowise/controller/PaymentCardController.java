package com.innowise.controller;

import com.innowise.dto.PageResponse;
import com.innowise.dto.PaymentCardDTO;
import com.innowise.mapper.PaymentCardMapper;
import com.innowise.model.PaymentCard;
import com.innowise.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class PaymentCardController {
    private final PaymentCardService paymentCardService;
    @PostMapping
    public ResponseEntity<PaymentCardDTO> createCard(@Valid @RequestBody PaymentCardDTO paymentCardDTO){
        PaymentCardDTO createdCardDTO = paymentCardService.createCard(paymentCardDTO);
        return new ResponseEntity<>(createdCardDTO, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardDTO> getCard(@PathVariable Long id){
        PaymentCardDTO cardDTO =  paymentCardService.getCardById(id);
        return ResponseEntity.ok(cardDTO);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PaymentCardDTO>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        PageResponse<PaymentCardDTO> cards = paymentCardService.getAllCards(page, size);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentCardDTO>> getCardsByUserId(@PathVariable Long userId){
        List<PaymentCardDTO> cards = paymentCardService.getCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardDTO> updateCard(@PathVariable Long id, @Valid @RequestBody PaymentCardDTO newCardDTO){
        PaymentCardDTO cardDTO = paymentCardService.updateCard(id, newCardDTO);
        return ResponseEntity.ok(cardDTO);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<PaymentCardDTO> activateCard(@PathVariable Long id){
        paymentCardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PaymentCardDTO> deactivateCard(@PathVariable Long id){
        paymentCardService.deactivateCard(id);
        return ResponseEntity.noContent().build();
    }
}
