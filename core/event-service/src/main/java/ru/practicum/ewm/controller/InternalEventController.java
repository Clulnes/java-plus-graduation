package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
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
    public EventInternalDto getEventById(@PathVariable("eventId") Long eventId) {
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
    @Transactional
    public void incrementConfirmedRequests(@PathVariable("eventId") Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        int current = event.getConfirmedRequests() == null ? 0 : event.getConfirmedRequests();
        event.setConfirmedRequests(current + 1);
        eventRepository.save(event);
    }

    @PostMapping("/{eventId}/decrement-confirmed")
    @Transactional
    public void decrementConfirmedRequests(@PathVariable("eventId") Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        int current = event.getConfirmedRequests() == null ? 0 : event.getConfirmedRequests();
        if (current > 0) {
            event.setConfirmedRequests(current - 1);
            eventRepository.save(event);
        }
    }

    @PostMapping("/{eventId}/add-confirmed")
    @Transactional
    public void addConfirmedRequests(@PathVariable("eventId") Long eventId, @RequestParam("count") Integer count) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        int current = event.getConfirmedRequests() == null ? 0 : event.getConfirmedRequests();
        event.setConfirmedRequests(current + count);
        eventRepository.save(event);
    }
}