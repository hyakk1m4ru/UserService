package com.innowise.unit.mapper;

import com.innowise.dto.UserDTO;
import com.innowise.model.User;
import com.innowise.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void mapEntityToDto() {
        User user = new User();
        user.setId(1L);
        user.setName("John");
        user.setSurname("Doe");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setEmail("john@example.com");
        user.setActive(true);

        UserDTO dto = userMapper.toDto(user);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("John");
        assertThat(dto.getSurname()).isEqualTo("Doe");
        assertThat(dto.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getActive()).isTrue();
    }

    @Test
    void mapDtoToEntity() {

        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setName("John");
        dto.setSurname("Doe");
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        dto.setEmail("john@example.com");
        dto.setActive(true);

        User user = userMapper.toEntity(dto);

        assertThat(user).isNotNull();
        assertThat(user.getId()).isNull();  // id игнорируется при создании
        assertThat(user.getName()).isEqualTo("John");
        assertThat(user.getSurname()).isEqualTo("Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void updateEntityFromDtoUpdateOnlyNonNullFields() {
        User user = new User();
        user.setId(1L);
        user.setName("John");
        user.setActive(true);

        UserDTO dto = new UserDTO();
        dto.setName("Johnny");
        dto.setSurname("Doe");

        userMapper.updateEntityFromDto(dto, user);

        assertThat(user.getId()).isEqualTo(1L);  // id не меняется
        assertThat(user.getName()).isEqualTo("Johnny");  // обновилось
        assertThat(user.getSurname()).isEqualTo("Doe");  // обновилось
        assertThat(user.getActive()).isTrue();  // active не изменилось (игнорируется)
    }
}