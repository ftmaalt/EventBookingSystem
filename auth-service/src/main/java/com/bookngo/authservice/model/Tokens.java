package com.bookngo.catalogservice.model.model;

import com.bookngo.catalogservice.model.model.enums.TokenType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="tokens")
public class Tokens {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tokens_id")
    private Long tokensId;

    @NotBlank
    @Column(name = "token", length = 100, nullable = false)
    private String token;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TokenType type;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    //    --- Relationship Mapping ---
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


}
