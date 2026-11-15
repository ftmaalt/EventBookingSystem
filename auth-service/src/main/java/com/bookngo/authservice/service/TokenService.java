package com.bookngo.authservice.service;

import com.bookngo.authservice.exception.InformationNotFoundException;
import com.bookngo.authservice.model.Tokens;
import com.bookngo.authservice.model.User;
import com.bookngo.authservice.model.enums.TokenType;
import com.bookngo.authservice.repository.TokensRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
public class TokenService {

    private final TokensRepository tokensRepository;

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    public TokenService(TokensRepository tokensRepository) {
        this.tokensRepository = tokensRepository;
    }

    @Transactional
    public String generateToken(User user, TokenType type) {
        if (user == null || type == null) {
            throw new IllegalArgumentException("User and token type are required.");
        }

        LocalDateTime expiresAt = switch (type) {
            case EMAIL_VERIFICATION -> LocalDateTime.now().plusHours(24);
            case PASSWORD_RESET -> LocalDateTime.now().plusMinutes(6);
            case PENALTY_PAYMENT -> LocalDateTime.now().plusMinutes(10);
            default -> throw new IllegalArgumentException("Unsupported token type: " + type);
        };
        String tokenString = UUID.randomUUID().toString();

        Tokens token = new Tokens();
        token.setToken(tokenString);
        token.setType(type);
        token.setUser(user);
        token.setExpiresAt(expiresAt);

        tokensRepository.save(token);
        logger.info("{} token generated for user ID: {}", type, user.getId());
        return tokenString;
    }

    @Transactional
    public User validateToken(String tokenString, TokenType expectedType) {
        logger.info("Validating {} token", expectedType);
        Tokens token = tokensRepository.findByTokenForUpdate(tokenString).orElseThrow(() -> new InformationNotFoundException("Invalid token."));
        if (token.getType() != expectedType) {
            throw new InformationNotFoundException("Invalid token.");
        }
        if (token.getUsedAt() != null) {
            throw new InformationNotFoundException("This token has already been used.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now)) {
            throw new InformationNotFoundException("This token has expired.");
        }

        token.setUsedAt(now);
        tokensRepository.save(token);
        logger.info("{} token validated for user ID: {}", expectedType, token.getUser().getId());
        return token.getUser();
    }

}