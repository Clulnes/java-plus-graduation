package ru.practicum.user.service;

import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto createUser(NewUserRequest request);

    void deleteUser(Long userId);

    UserDto getUserById(Long userId);

    boolean existsById(Long userId);
}