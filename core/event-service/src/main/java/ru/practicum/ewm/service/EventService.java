package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.*;

import java.util.List;

public interface EventService {

    EventFullDto addEvent(Long userId, NewEventDto dto);

    EventFullDto getPrivateEvent(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventShortDto> getPrivateEvents(long userId, int from, int size);

    List<EventShortDto> getPublicEvents(EventSearchParams params,
                                        HttpServletRequest request);

    List<EventFullDto> getEventsByAdmin(AdminEventSearchParams params);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    EventFullDto getPublicEventById(Long id, Long userId, HttpServletRequest request);

    List<EventShortDto> getRecommendations(Long userId, int size);

    void addLike(Long userId, Long eventId);
}