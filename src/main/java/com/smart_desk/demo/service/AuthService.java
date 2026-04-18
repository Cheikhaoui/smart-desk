package com.smart_desk.demo.service;

import com.smart_desk.demo.dto.AuthDto;
import com.smart_desk.demo.dto.UserDto;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.repositories.UserRepository;
import com.smart_desk.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthDto.TokenResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered: " + req.email());
        }

        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role(User.Role.USER)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        return new AuthDto.TokenResponse(jwtService.generateToken(saved), UserDto.Response.from(saved));
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        User user = userRepository.findByEmail(req.email()).orElseThrow();
        return new AuthDto.TokenResponse(jwtService.generateToken(user), UserDto.Response.from(user));
    }
}
