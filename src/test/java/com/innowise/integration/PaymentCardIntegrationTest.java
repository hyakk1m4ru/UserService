package com.innowise.integration;

import com.innowise.dto.PaymentCardDTO;
import com.innowise.dto.UserDTO;
import com.innowise.repository.PaymentCardRepository;
import com.innowise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentCardControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.cache.type", () -> "none");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        paymentCardRepository.deleteAll();
        createTestUser();
    }

    @Test
    void createCardReturnsCreatedCard() {
        PaymentCardDTO cardDTO = createCardDTO();

        ResponseEntity<PaymentCardDTO> response = restTemplate.postForEntity("/api/cards", cardDTO, PaymentCardDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getNumber()).isEqualTo("1234567890123456");
    }

    @Test
    void getCardByIdReturnsCard() {
        createTestCard();

        ResponseEntity<PaymentCardDTO> response = restTemplate.getForEntity("/api/cards/1", PaymentCardDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void getCardsByUserIdReturnsCardList() {
        createTestCard();

        ResponseEntity<PaymentCardDTO[]> response = restTemplate.getForEntity("/api/cards/user/1", PaymentCardDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
    }

    @Test
    void createCardLimitReturnsBadRequest() {
        for (int i = 0; i < 5; i++) {
            PaymentCardDTO cardDTO = createCardDTOWithNumber("123456789012345" + i);
            restTemplate.postForEntity("/api/cards", cardDTO, PaymentCardDTO.class);
        }

        PaymentCardDTO sixthCard = createCardDTOWithNumber("9999999999999999");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/cards", sixthCard, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateCardReturnsUpdatedCard() {
        createTestCard();

        PaymentCardDTO updateDTO = createCardDTO();
        updateDTO.setHolder("John Smith");

        restTemplate.put("/api/cards/1", updateDTO);
        ResponseEntity<PaymentCardDTO> response = restTemplate.getForEntity("/api/cards/1", PaymentCardDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getHolder()).isEqualTo("John Smith");
    }

    private void createTestUser() {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("John");
        userDTO.setSurname("Doe");
        userDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        userDTO.setEmail("john@example.com");
        restTemplate.postForEntity("/api/users", userDTO, UserDTO.class);
    }

    private void createTestCard() {
        PaymentCardDTO cardDTO = createCardDTO();
        restTemplate.postForEntity("/api/cards", cardDTO, PaymentCardDTO.class);
    }

    private PaymentCardDTO createCardDTO() {
        PaymentCardDTO cardDTO = new PaymentCardDTO();
        cardDTO.setNumber("1234567890123456");
        cardDTO.setHolder("John Doe");
        cardDTO.setExpirationDate(LocalDate.of(2028, 12, 31));
        cardDTO.setUserId(1L);
        return cardDTO;
    }

    private PaymentCardDTO createCardDTOWithNumber(String number) {
        PaymentCardDTO cardDTO = new PaymentCardDTO();
        cardDTO.setNumber(number);
        cardDTO.setHolder("John Doe");
        cardDTO.setExpirationDate(LocalDate.of(2028, 12, 31));
        cardDTO.setUserId(1L);
        return cardDTO;
    }
}