package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.UserDto;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {

    @GetMapping("/{userId}/exists")
    Boolean existsById(@PathVariable("userId") Long userId);

    @GetMapping("/{userId}")
    UserDto getUserById(@PathVariable("userId") Long userId);
}