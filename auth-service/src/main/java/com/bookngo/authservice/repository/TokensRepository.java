package com.bookngo.authservice.repository;
import com.bookngo.authservice.model.Tokens;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface TokensRepository extends JpaRepository<Tokens, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)

    @Query("SELECT t FROM Tokens t WHERE t.token = :token")
    Optional<Tokens> findByTokenForUpdate(@Param("token") String token);
    Optional<Tokens> findByToken(String token);

    @Transactional
    void deleteAllByUser_Id(Long userId);
}