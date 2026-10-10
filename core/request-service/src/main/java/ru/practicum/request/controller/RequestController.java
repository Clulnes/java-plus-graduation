package ru.practicum.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class RequestController {

    private final RequestServiceImpl requestService;

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getRequestsByUserId(@PathVariable Long userId) {
        return requestService.getRequestsByUserId(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto sendRequest(@PathVariable Long userId, @RequestParam Long eventId) {
        return requestService.sendRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Long userId, @PathVariable Long eventId) {
        return requestService.getRequestsByEventId(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateOwnParticipationRequests(@PathVariable Long userId,
                                                                         @PathVariable Long eventId,
                                                                         @Valid @RequestBody EventRequestStatusUpdateRequest request) {
        return requestService.updateOwnParticipationRequests(userId, eventId, request);
    }

    @GetMapping("/internal/requests/check-confirmed")
    public Boolean checkConfirmed(@RequestParam Long userId, @RequestParam Long eventId) {
        return requestService.hasConfirmedRequest(userId, eventId);
    }
}