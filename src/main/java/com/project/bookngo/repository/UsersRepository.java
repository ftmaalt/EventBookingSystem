package com.project.bookngo.repository;

import com.project.bookngo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<User, Long> {
    User findUserById(Long id);
    User findUserByEmail(String email);
    boolean existsByEmail(String email);
    User findUserByIdAndFullName(Long id, String fullname);
    User findUserByPhone(String phone);
}