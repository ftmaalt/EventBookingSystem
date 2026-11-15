package com.bookngo.authservice.repository;


import com.bookngo.authservice.model.User;
import com.bookngo.authservice.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, Long> {
    User findUserById(Long id);
    List<User> findByRole(UserRole role);
    Optional<User> findUserByEmail(String email);
    boolean existsByEmail(String email);
    User findUserByIdAndFullName(Long id, String fullname);
    User findUserByPhone(String phone);
}