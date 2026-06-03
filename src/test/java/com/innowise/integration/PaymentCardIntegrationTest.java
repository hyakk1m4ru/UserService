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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentCardIntegrationTest {

    static {
        System.setProperty("testcontainers.docker.client.strategy", "org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy");
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    private Long testUserId;
    private Long testCardId;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        } catch (Exception ignored) {
        }
        testUserId = createTestUser();
        testCardId = createTestCard();
    }

    @Test
    void createCardReturnsCreatedCard() {
        PaymentCardDTO cardDTO = createCardDTOWithNumber("9999999999999999");
        cardDTO.setUserId(testUserId);

        ResponseEntity<PaymentCardDTO> response = restTemplate.postForEntity("/api/cards", cardDTO, PaymentCardDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getNumber()).isEqualTo("9999999999999999");
        assertThat(response.getBody().getActive()).isTrue();
    }

    @Test
    void getCardByIdReturnsCard() {
        ResponseEntity<PaymentCardDTO> response = restTemplate.getForEntity("/api/cards/" + testCardId, PaymentCardDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testCardId);
    }

    @Test
    void getCardsByUserIdReturnsCardList() {
        ResponseEntity<List<PaymentCardDTO>> response = restTemplate.exchange(
                "/api/cards/user/" + testUserId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    void createCardLimitReturnsBadRequest() {
        for (int i = 0; i < 4; i++) {
            PaymentCardDTO cardDTO = createCardDTOWithNumber("123456789012345" + i);
            cardDTO.setUserId(testUserId);
            ResponseEntity<PaymentCardDTO> response = restTemplate.postForEntity("/api/cards", cardDTO, PaymentCardDTO.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        PaymentCardDTO sixthCard = createCardDTOWithNumber("9999999999999999");
        sixthCard.setUserId(testUserId);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/cards", sixthCard, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("cant have >= 5 active cards");
    }

    @Test
    void updateCardReturnsUpdatedCard() {
        PaymentCardDTO updateDTO = createCardDTO();
        updateDTO.setHolder("John Smith");
        updateDTO.setUserId(testUserId);

        restTemplate.put("/api/cards/" + testCardId, updateDTO);
        ResponseEntity<PaymentCardDTO> response = restTemplate.getForEntity("/api/cards/" + testCardId, PaymentCardDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getHolder()).isEqualTo("John Smith");
    }

    @Test
    void activateCardUpdatesStatus() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

        restTemplate.exchange("/api/cards/" + testCardId + "/deactivate",
                HttpMethod.PATCH, requestEntity, Void.class);

        ResponseEntity<PaymentCardDTO> deactivatedResponse = restTemplate.getForEntity("/api/cards/" + testCardId, PaymentCardDTO.class);
        assertThat(deactivatedResponse.getBody().getActive()).isFalse();

        restTemplate.exchange("/api/cards/" + testCardId + "/activate",
                HttpMethod.PATCH, requestEntity, Void.class);

        ResponseEntity<PaymentCardDTO> response = restTemplate.getForEntity("/api/cards/" + testCardId, PaymentCardDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getActive()).isTrue();
    }

    @Test
    void deactivateCardUpdatesStatus() {
        ResponseEntity<PaymentCardDTO> activeResponse = restTemplate.getForEntity("/api/cards/" + testCardId, PaymentCardDTO.class);
        assertThat(activeResponse.getBody().getActive()).isTrue();

        HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

        restTemplate.exchange("/api/cards/" + testCardId + "/deactivate",
                HttpMethod.PATCH, requestEntity, Void.class);

        ResponseEntity<PaymentCardDTO> response = restTemplate.getForEntity("/api/cards/" + testCardId, PaymentCardDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getActive()).isFalse();
    }

    @Test
    void createCardWithDuplicateNumberReturnsBadRequest() {
        PaymentCardDTO duplicateCard = createCardDTO();
        duplicateCard.setUserId(testUserId);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/cards", duplicateCard, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("already exists");
    }

    @Test
    void getCardByNonExistentIdReturnsNotFound() {
        ResponseEntity<PaymentCardDTO> response = restTemplate.getForEntity("/api/cards/99999", PaymentCardDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Long createTestUser() {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("John");
        userDTO.setSurname("Doe");
        userDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        userDTO.setEmail("john@example.com");

        ResponseEntity<UserDTO> response = restTemplate.postForEntity("/api/users", userDTO, UserDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getId();
    }

    private Long createTestCard() {
        PaymentCardDTO cardDTO = createCardDTO();
        cardDTO.setUserId(testUserId);

        ResponseEntity<PaymentCardDTO> response = restTemplate.postForEntity("/api/cards", cardDTO, PaymentCardDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getId();
    }

    private PaymentCardDTO createCardDTO() {
        PaymentCardDTO cardDTO = new PaymentCardDTO();
        cardDTO.setNumber("1234567890123456");
        cardDTO.setHolder("John Doe");
        cardDTO.setExpirationDate(LocalDate.of(2028, 12, 31));
        return cardDTO;
    }

    private PaymentCardDTO createCardDTOWithNumber(String number) {
        PaymentCardDTO cardDTO = new PaymentCardDTO();
        cardDTO.setNumber(number);
        cardDTO.setHolder("John Doe");
        cardDTO.setExpirationDate(LocalDate.of(2028, 12, 31));
        return cardDTO;
    }
}