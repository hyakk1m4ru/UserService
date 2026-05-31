package com.innowise.integration;

import com.innowise.dto.UserDTO;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class UserControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_ReturnsCreatedUser() {
        // Given
        UserDTO userDTO = new UserDTO();
        userDTO.setName("John");
        userDTO.setSurname("Doe");
        userDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        userDTO.setEmail("john@example.com");

        // When
        ResponseEntity<UserDTO> response = restTemplate.postForEntity("/api/users", userDTO, UserDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("john@example.com");
        assertThat(response.getBody().getActive()).isTrue();
    }

    @Test
    void getUserById_ReturnsUser() {
        // Given
        UserDTO userDTO = createTestUser();

        // When
        ResponseEntity<UserDTO> response = restTemplate.getForEntity("/api/users/1", UserDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void getAllUsers_ReturnsPageOfUsers() {
        // Given
        createTestUser();
        createTestUser2();

        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users?page=0&size=10", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content");
        assertThat(response.getBody()).contains("totalElements");
    }

    @Test
    void updateUser_ReturnsUpdatedUser() {
        // Given
        createTestUser();

        UserDTO updateDTO = new UserDTO();
        updateDTO.setName("Johnny");
        updateDTO.setSurname("Doe");
        updateDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        updateDTO.setEmail("johnny@example.com");

        // When
        restTemplate.put("/api/users/1", updateDTO);
        ResponseEntity<UserDTO> response = restTemplate.getForEntity("/api/users/1", UserDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Johnny");
        assertThat(response.getBody().getEmail()).isEqualTo("johnny@example.com");
    }

    @Test
    void deleteUser_ReturnsNoContent() {
        // Given
        createTestUser();

        // When
        restTemplate.delete("/api/users/1");

        // Then
        ResponseEntity<UserDTO> response = restTemplate.getForEntity("/api/users/1", UserDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void activateUser_UpdatesStatus() {
        // Given
        createTestUser();

        // When
        restTemplate.patchForObject("/api/users/1/activate", null, Void.class);

        // Then
        ResponseEntity<UserDTO> response = restTemplate.getForEntity("/api/users/1", UserDTO.class);
        assertThat(response.getBody().getActive()).isTrue();
    }

    private UserDTO createTestUser() {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("John");
        userDTO.setSurname("Doe");
        userDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        userDTO.setEmail("john@example.com");
        restTemplate.postForEntity("/api/users", userDTO, UserDTO.class);
        return userDTO;
    }

    private void createTestUser2() {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("Jane");
        userDTO.setSurname("Smith");
        userDTO.setBirthDate(LocalDate.of(1995, 5, 5));
        userDTO.setEmail("jane@example.com");
        restTemplate.postForEntity("/api/users", userDTO, UserDTO.class);
    }
}
