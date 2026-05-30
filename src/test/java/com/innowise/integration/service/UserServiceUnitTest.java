package com.innowise.integration.service;

import com.innowise.dto.UserDTO;
import com.innowise.exception.BusinessException;
import com.innowise.exception.ResourceNotFoundException;
import com.innowise.mapper.UserMapper;
import com.innowise.model.User;
import com.innowise.repository.UserRepository;
import com.innowise.service.UserService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setSurname("Doe");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setEmail("john@example.com");
        user.setActive(true);

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setName("John");
        userDTO.setSurname("Doe");
        userDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        userDTO.setEmail("john@example.com");
        userDTO.setActive(true);
    }
    @Test
    void createUserTest() {
        when(userRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(userMapper.toEntity(any(UserDTO.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDTO);

        UserDTO result = userService.createUser(userDTO);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }
    @Test
    void createUserEmailAlreadyExistsThrowsException() {
        when(userRepository.existsByEmail(userDTO.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(userDTO))
                .isInstanceOf(BusinessException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserByIdTest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDTO);

        UserDTO result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John");
    }
    @Test
    void getUserByIdNotFoundThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void updateUserTest() {
        UserDTO updateDTO = new UserDTO();
        updateDTO.setName("Johnny");
        updateDTO.setSurname("Doe");
        updateDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        updateDTO.setEmail("johnny@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("johnny@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(updateDTO);

        UserDTO result = userService.updateUser(1L, updateDTO);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("johnny@example.com");
    }
    @Test
    void deleteUserSuccessTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }
    @Test
    void deleteUserNotFoundThrowsException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void activateUserTest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDTO);

        UserDTO result = userService.activateUser(1L);

        assertThat(result).isNotNull();
        assertThat(result.getActive()).isTrue();
    }
}
