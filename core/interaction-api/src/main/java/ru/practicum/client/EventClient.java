package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventInternalDto;

@FeignClient(name = "main-service", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventInternalDto getEventById(@PathVariable("eventId") Long eventId);

    @PostMapping("/{eventId}/increment-confirmed")
    void incrementConfirmedRequests(@PathVariable("eventId") Long eventId);

    @PostMapping("/{eventId}/decrement-confirmed")
    void decrementConfirmedRequests(@PathVariable("eventId") Long eventId);

    @PostMapping("/{eventId}/add-confirmed")
    void addConfirmedRequests(@PathVariable("eventId") Long eventId, @RequestParam("count") Integer count);
}