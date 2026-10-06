package com.project.bookngo.repository;

import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsersRepository extends JpaRepository<User, Long> {
    User findUserById(Long id);
    List<User> findByRole(UserRole role);
    User findUserByEmail(String email);
    boolean existsByEmail(String email);
    User findUserByIdAndFullName(Long id, String fullname);
    User findUserByPhone(String phone);
}