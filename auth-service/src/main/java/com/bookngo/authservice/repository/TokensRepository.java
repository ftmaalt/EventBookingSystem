package com.bookngo.authservice.repository;
import com.bookngo.authservice.model.Tokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface TokensRepository extends JpaRepository<Tokens, Long> {
    Optional<Tokens> findByToken(String token);
    @Transactional
    void deleteAllByUser_Id(Long userId);
}