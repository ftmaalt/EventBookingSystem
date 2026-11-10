package com.bookngo.authservice.security;

import com.bookngo.catalogservice.model.model.User;
import com.bookngo.authservice.repository.UsersRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MyUserDetailsService  implements UserDetailsService {
    private UsersRepository usersRepository;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user= usersRepository.findUserByEmail(email).orElseThrow();
        if (user==null){
            throw new UsernameNotFoundException("No user with email: "+ email+" exists. Please try again using another email");
        }

        return new MyUserDetails(user);
    }
}
