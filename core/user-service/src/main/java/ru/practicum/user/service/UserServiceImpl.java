package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.user.dao.UserRepository;
import ru.practicum.user.exception.ConflictException;
import ru.practicum.user.exception.NotFoundException;
import ru.practicum.user.model.User;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        if (ids != null && !ids.isEmpty()) {
            return userRepository.findByIdIn(ids, page).map(this::toDto).toList();
        }
        return userRepository.findAll(page).map(this::toDto).toList();
    }

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
        user = userRepository.save(user);
        log.info("User created: {}", user.getId());
        return toDto(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);
    }

    @Override
    public UserDto getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}