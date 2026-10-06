package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventInternalDto;
import ru.practicum.ewm.dao.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.Event;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/{eventId}")
    public EventInternalDto getEventById(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Field: eventId. Error: event не найден. Value: " + eventId));

        return EventInternalDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .initiatorId(event.getInitiator().getId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .confirmedRequests(event.getConfirmedRequests())
                .requestModeration(event.getRequestModeration())
                .build();
    }

    @PostMapping("/{eventId}/increment-confirmed")
    public void incrementConfirmedRequests(@PathVariable Long eventId) {
        eventRepository.updateIncrementConfirmedRequests(eventId);
    }

    @PostMapping("/{eventId}/decrement-confirmed")
    public void decrementConfirmedRequests(@PathVariable Long eventId) {
        eventRepository.updateDecrementConfirmedRequests(eventId);
    }

    @PostMapping("/{eventId}/add-confirmed")
    public void addConfirmedRequests(@PathVariable Long eventId, @RequestParam Integer count) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        event.setConfirmedRequests(event.getConfirmedRequests() + count);
        eventRepository.save(event);
    }
}