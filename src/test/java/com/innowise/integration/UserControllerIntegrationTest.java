package com.innowise.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.dto.UserDTO;
import com.innowise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {

    static {
        System.setProperty("testcontainers.docker.client.strategy", "org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy");
    }
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        } catch (Exception ignored) {
        }
    }



    @Test
    void createUser_ReturnsCreatedUser() throws Exception {
        // Given
        UserDTO userDTO = new UserDTO();
        userDTO.setName("John");
        userDTO.setSurname("Doe");
        userDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        userDTO.setEmail("john@example.com");

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        UserDTO createdUser = objectMapper.readValue(result.getResponse().getContentAsString(), UserDTO.class);
        assertThat(createdUser.getId()).isNotNull();
    }

    @Test
    void getUserById_ReturnsUser() throws Exception {
        // Given
        UserDTO createdUser = createTestUser();

        // When & Then
        mockMvc.perform(get("/api/users/{id}", createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdUser.getId()))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    void getAllUsers_ReturnsPageOfUsers() throws Exception {
        // Given
        createTestUser();
        createTestUser2();

        // When & Then
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].email").value("jane@example.com")); // проверка сортировки по createdAt DESC
    }

    @Test
    void getAllUsers_WithFilters_ReturnsFilteredUsers() throws Exception {
        // Given
        createTestUser();
        createTestUser2();

        // When & Then - фильтр по имени
        mockMvc.perform(get("/api/users")
                        .param("name", "John")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("John"));
    }

    @Test
    void updateUser_ReturnsUpdatedUser() throws Exception {
        // Given
        UserDTO createdUser = createTestUser();

        UserDTO updateDTO = new UserDTO();
        updateDTO.setName("Johnny");
        updateDTO.setSurname("Doe");
        updateDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        updateDTO.setEmail("johnny@example.com");

        // When & Then
        mockMvc.perform(put("/api/users/{id}", createdUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Johnny"))
                .andExpect(jsonPath("$.email").value("johnny@example.com"));

        // Verify
        mockMvc.perform(get("/api/users/{id}", createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Johnny"))
                .andExpect(jsonPath("$.email").value("johnny@example.com"));
    }

    @Test
    void updateUser_WithDuplicateEmail_ReturnsBadRequest() throws Exception {
        // Given
        UserDTO user1 = createTestUser();
        UserDTO user2 = new UserDTO();
        user2.setName("Jane");
        user2.setSurname("Smith");
        user2.setBirthDate(LocalDate.of(1995, 5, 5));
        user2.setEmail("jane@example.com");

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated())
                .andReturn();

        UserDTO createdUser2 = objectMapper.readValue(result.getResponse().getContentAsString(), UserDTO.class);

        // Try to update user2 with user1's email
        UserDTO updateDTO = new UserDTO();
        updateDTO.setName("Jane");
        updateDTO.setSurname("Smith");
        updateDTO.setBirthDate(LocalDate.of(1995, 5, 5));
        updateDTO.setEmail("john@example.com"); // duplicate email

        // When & Then
        mockMvc.perform(put("/api/users/{id}", createdUser2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_ReturnsNoContent() throws Exception {
        // Given
        UserDTO createdUser = createTestUser();

        // When
        mockMvc.perform(delete("/api/users/{id}", createdUser.getId()))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get("/api/users/{id}", createdUser.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateUser_UpdatesStatus() throws Exception {
        // Given
        UserDTO createdUser = createTestUser();

        // First deactivate
        mockMvc.perform(patch("/api/users/{id}/deactivate", createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // When - activate
        mockMvc.perform(patch("/api/users/{id}/activate", createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        // Verify
        mockMvc.perform(get("/api/users/{id}", createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivateUser_UpdatesStatus() throws Exception {
        // Given
        UserDTO createdUser = createTestUser();
        assertThat(createdUser.getActive()).isTrue();

        // When
        mockMvc.perform(patch("/api/users/{id}/deactivate", createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Verify
        mockMvc.perform(get("/api/users/{id}", createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void getUserById_WithNonExistentId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    private UserDTO createTestUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("John");
        userDTO.setSurname("Doe");
        userDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        userDTO.setEmail("john@example.com");

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), UserDTO.class);
    }

    private void createTestUser2() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("Jane");
        userDTO.setSurname("Smith");
        userDTO.setBirthDate(LocalDate.of(1995, 5, 5));
        userDTO.setEmail("jane@example.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated());
    }
}