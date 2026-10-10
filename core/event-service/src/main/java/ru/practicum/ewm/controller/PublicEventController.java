package ru.practicum.ewm.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventSearchParams;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(@ModelAttribute EventSearchParams params,
                                         HttpServletRequest request) {

        log.info("GET /events params={}", params);

        return eventService.getPublicEvents(params, request);
    }

    @GetMapping("/{id}")
    public EventFullDto getPublicEventById(
            @PathVariable Long id,
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId,
            HttpServletRequest request) {
        return eventService.getPublicEventById(id, userId, request);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") int size) {
        return eventService.getRecommendations(userId, size);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void addLike(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {
        eventService.addLike(userId, eventId);
    }
}