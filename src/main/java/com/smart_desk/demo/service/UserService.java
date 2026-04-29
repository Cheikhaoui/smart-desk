package com.smart_desk.demo.service;

import com.smart_desk.demo.dto.UserDto;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public List<UserDto.UserSummary> search(User.Role role, boolean active) {
        return userRepository.search(role, active).stream()
                .map(UserDto.UserSummary::from)
                .toList();
    }
}
