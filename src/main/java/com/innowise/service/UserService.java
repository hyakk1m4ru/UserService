package com.innowise.service;

import com.innowise.dto.PageResponse;
import com.innowise.dto.UserDTO;
import com.innowise.exception.BusinessException;
import com.innowise.exception.ResourceNotFoundException;
import com.innowise.mapper.UserMapper;
import com.innowise.model.User;
import com.innowise.repository.UserRepository;
import com.innowise.specification.UserSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @CacheEvict(value = {"users", "userPage"}, allEntries = true)
    public UserDTO createUser(UserDTO userDTO){
        if(userRepository.existsByEmail(userDTO.getEmail())){
            throw new BusinessException("user with email "+userDTO.getEmail() + " already exists");
        }
        User user = userMapper.toEntity(userDTO);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Cacheable(value = "user", key = "#id", unless = "#result == null")
    public UserDTO getUserById(Long id){
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toDto(user);
    }

    @Cacheable(value = "userPage", key = "{#name, #surname, #email, #active, #page, #size}")
    public PageResponse<UserDTO> getAllUsers(String name, String surname, String email, Boolean active, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<User> spec = UserSpecification.filterBy(name, surname, email, active);
        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<UserDTO> userDTOS = userPage.getContent().stream().map(userMapper::toDto).toList();
        return new PageResponse<>(userDTOS, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements());
    }
    @CachePut(value = "user", key = "#id")
    @CacheEvict(value = "userPage", allEntries = true)
    @Transactional
    public UserDTO activateUser(Long id){
        return updateUserActiveStatus(id, true);
    }
    @CachePut(value = "user", key = "#id")
    @CacheEvict(value = "userPage", allEntries = true)
    @Transactional
    public UserDTO deactivateUser(Long id){

        return updateUserActiveStatus(id, false);
    }

    @Transactional
    @CachePut(value = "user", key = "#id")
    @CacheEvict(value = "userPage",allEntries = true)
    public UserDTO updateUser(Long id, UserDTO userDTO){
        User user = userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User", id));
        if(!user.getEmail().equals(userDTO.getEmail()) && userRepository.existsByEmail(userDTO.getEmail())){
            throw new BusinessException("Email "+userDTO.getEmail()+" is already taken");
        }
        userMapper.updateEntityFromDto(userDTO, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }
    @Transactional
    @CacheEvict(value = {"user", "userPage"}, allEntries = true)
    public void deleteUser(Long id){
        if(!userRepository.existsById(id)){
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }
    private UserDTO updateUserActiveStatus(Long id, Boolean active) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("user", id));
        user.setActive(active);
        User savedUser = userRepository.save(user);

        if(!active){
            user.getPaymentCards().forEach(card -> card.setActive(false));
        }
        return userMapper.toDto(savedUser);
    }
}
