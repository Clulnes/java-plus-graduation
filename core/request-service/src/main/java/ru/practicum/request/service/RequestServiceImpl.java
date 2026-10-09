package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.EventInternalDto;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.EventState;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.RequestStatus;
import ru.practicum.request.dao.RequestRepository;
import ru.practicum.request.exception.ConflictException;
import ru.practicum.request.exception.NotFoundException;
import ru.practicum.request.exception.ValidationException;
import ru.practicum.request.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl {

    private final RequestRepository requestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;

    public List<ParticipationRequestDto> getRequestsByEventId(Long ownerId, Long eventId) {
        getEventIfExistWithOwnerValidation(eventId, ownerId);

        return requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EventRequestStatusUpdateResult updateOwnParticipationRequests(Long ownerId, Long eventId,
                                                                         EventRequestStatusUpdateRequest request) {
        EventInternalDto event = getEventIfExistWithOwnerValidation(eventId, ownerId);

        long limit = event.getParticipantLimit();
        if (limit == 0) {
            throw new ConflictException("The participant limit is 0, moderation is disabled");
        }
        if (!event.getRequestModeration()) {
            throw new ConflictException("The request moderation is disabled");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);

        long countConfirmed = requests.stream()
                .filter(req -> req.getStatus() == RequestStatus.CONFIRMED)
                .count();

        if (countConfirmed >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }

        Set<Long> idsToCheck = new HashSet<>(request.getRequestIds());
        boolean allPending = requests.stream()
                .filter(req -> idsToCheck.contains(req.getId()))
                .allMatch(req -> req.getStatus() == RequestStatus.PENDING);

        if (!allPending) {
            throw new ValidationException("Request must have status PENDING");
        }

        List<ParticipationRequest> updatedRequests = new ArrayList<>();
        List<ParticipationRequest> toConfirm = new ArrayList<>();
        List<ParticipationRequest> toReject;

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            long availableSlots = limit - countConfirmed;

            List<ParticipationRequest> targetRequests = requests.stream()
                    .filter(req -> request.getRequestIds().contains(req.getId()))
                    .toList();

            toConfirm = targetRequests.stream().limit(availableSlots).toList();
            toReject = targetRequests.stream().skip(availableSlots).toList();

            toConfirm.forEach(req -> req.setStatus(RequestStatus.CONFIRMED));
            toReject.forEach(req -> req.setStatus(RequestStatus.REJECTED));

        } else if (request.getStatus() == RequestStatus.REJECTED) {
            toReject = requests.stream()
                    .filter(req -> request.getRequestIds().contains(req.getId()))
                    .toList();
            toReject.forEach(req -> req.setStatus(RequestStatus.REJECTED));
        } else {
            throw new ValidationException("Field: status. Error: must be CONFIRMED or REJECTED. Value: " + request.getStatus());
        }

        updatedRequests.addAll(toConfirm);
        updatedRequests.addAll(toReject);

        int newConfirmedRequests = toConfirm.size();
        if (newConfirmedRequests > 0) {
            eventClient.addConfirmedRequests(event.getId(), newConfirmedRequests);
        }

        requestRepository.saveAll(updatedRequests);

        List<ParticipationRequestDto> confirmed = toConfirm.stream().map(this::toDto).toList();
        List<ParticipationRequestDto> rejected = toReject.stream().map(this::toDto).toList();

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    public List<ParticipationRequestDto> getRequestsByUserId(Long userId) {
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ParticipationRequestDto sendRequest(Long userId, Long eventId) {
        EventInternalDto event = eventClient.getEventById(eventId);

        if (event.getState() !=  EventState.PUBLISHED) {
            throw new ConflictException("Field: eventId. Error: event не найден. Value: " + eventId);
        }

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("The request to own event is rejected");
        }

        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        int confirmedRequests = event.getConfirmedRequests() == null ? 0 : event.getConfirmedRequests();
        if (limit > 0 && confirmedRequests >= limit) {
            throw new ConflictException("The participant limit has been reached to event id: " + eventId);
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("The request to event id: " + eventId + " from user id: " + userId + " is already exists.");
        }

        if (!userClient.existsById(userId)) {
            throw new NotFoundException("User id: " + userId + " not found.");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .eventId(eventId)
                .requesterId(userId)
                .created(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();

        if (Boolean.FALSE.equals(event.getRequestModeration()) || limit == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            try {
                eventClient.incrementConfirmedRequests(event.getId());
            } catch (Exception e) {
                log.warn("Failed to increment confirmed requests count for event {}: {}", event.getId(), e.getMessage());
            }
        }

        request = requestRepository.save(request);
        return toDto(request);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id= " + requestId + " was not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new NotFoundException("Request with id= " + requestId + " was not found");
        }

        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);

        return toDto(request);
    }

    private EventInternalDto getEventIfExistWithOwnerValidation(Long eventId, Long userId) {
        EventInternalDto event = eventClient.getEventById(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Field: userId. Error: Initiator has another id. Value: " + userId);
        }
        return event;
    }

    private ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .created(request.getCreated())
                .build();
    }
}