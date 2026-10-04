package com.project.bookngo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.bookngo.model.enums.TokenType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Timestamp;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="tokens")
public class Tokens {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokens_id;

    @NotBlank
    @Column(length = 100)
    private String token;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column
    private TokenType type;

    @Timestamp
    @Column
    private LocalDateTime expires_at;

    @Timestamp
    @Column
    private LocalDateTime used_at;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created_at;

    //    --- Relationship Mapping ---
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private User user;


}
