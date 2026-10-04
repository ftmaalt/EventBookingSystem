package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Tokens;
import com.project.bookngo.model.enums.TokenType;
import com.project.bookngo.model.User;
import com.project.bookngo.repository.TokensRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.project.bookngo.model.enums.TokenType.EMAIL_VERIFICATION;
import static com.project.bookngo.model.enums.TokenType.PASSWORD_RESET;

@Service
public class TokenService {

    @Autowired
    private TokensRepository tokensRepository;


    public String generateToken(User user, TokenType type) {
       String tokenString = UUID.randomUUID().toString();
        // 2. create a new Tokens entity
        Tokens token = new Tokens();
        token.setToken(tokenString);
        token.setType(type);
        token.setUser(user);
        //    - set user, token (the random string), type
        if (type == EMAIL_VERIFICATION) {
            token.setExpires_at(LocalDateTime.now().plusHours(2));
        }else if(type == PASSWORD_RESET){
            token.setExpires_at(LocalDateTime.now().plusMinutes(6));
        }
        tokensRepository.save(token);
        return tokenString;
    }

    public User validateToken(String tokenString, TokenType expectedType) {
        Tokens token = tokensRepository.findByToken(tokenString).orElseThrow(() -> new InformationNotFoundException("Invalid token."));

        if (token.getType() != expectedType) {
            throw new InformationNotFoundException("Invalid token.");
        }
        if (token.getUsed_at() != null) {
            throw new InformationNotFoundException("This token has already been used.");
        }
        if (token.getExpires_at().isBefore(LocalDateTime.now())) {
            throw new InformationNotFoundException("This token has expired.");
        }
        token.setUsed_at(LocalDateTime.now());
        tokensRepository.save(token);
            return token.getUser();
        }

}