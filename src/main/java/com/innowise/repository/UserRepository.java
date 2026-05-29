package com.innowise.repository;
import com.innowise.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findBySurnameContainingIgnoreCase(String surname);

    Page<User> findByActive(Boolean active, Pageable pageable);

    boolean existsByEmail(String email);

    @Query(value = "SELECT * FROM users WHERE email = :email AND active = true",
            nativeQuery = true)
    Optional<User> findActiveUserByEmailNative(@Param("email") String email);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET active = :active WHERE id = :userId",
            nativeQuery = true)
    int updateUserActiveStatusNative(@Param("userId") Long userId,
                                     @Param("active") Boolean active);

    @Query(value = "SELECT * FROM users WHERE birth_date < :date",
            nativeQuery = true)
    List<User> findUsersBornBeforeDateNative(@Param("date") LocalDate date);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailJPQL(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.name LIKE %:name% OR u.surname LIKE %:surname%")
    Page<User> searchByNameOrSurnameJPQL(@Param("name") String name,
                                         @Param("surname") String surname,
                                         Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsersJPQL();

    @Query("SELECT u.id, u.name, u.surname, u.email FROM User u WHERE u.id = :userId")
    Optional<Object[]> findUserBasicInfoByIdJPQL(@Param("userId") Long userId);
}