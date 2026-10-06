package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.comment.dao.CommentRepository;
import ru.practicum.comment.dto.CommentResponseDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.exception.ConflictException;
import ru.practicum.comment.exception.NotFoundException;
import ru.practicum.comment.model.Comment;
import ru.practicum.dto.EventInternalDto;
import ru.practicum.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl {

    private final CommentRepository commentRepository;
    private final EventClient eventClient;
    private final UserClient userClient;

    @Transactional
    public CommentResponseDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        UserDto user = userClient.getUserById(userId);
        EventInternalDto event = eventClient.getEventById(eventId);

        if (!"PUBLISHED".equalsIgnoreCase(event.getState())) {
            throw new ConflictException("Cannot comment on an unpublished event");
        }

        if (dto.getAnswerTo() != null) {
            Comment parent = commentRepository.findById(dto.getAnswerTo())
                    .orElseThrow(() -> new NotFoundException("Parent comment id=" + dto.getAnswerTo() + " not found"));
            if (!parent.getEventId().equals(eventId)) {
                throw new ConflictException("Parent comment belongs to another event");
            }
        }

        Comment comment = Comment.builder()
                .eventId(eventId)
                .authorId(userId)
                .answerTo(dto.getAnswerTo())
                .text(dto.getText())
                .createdOn(LocalDateTime.now())
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment created: id={}", comment.getId());

        String displayName = userId.equals(event.getInitiatorId()) ? event.getTitle() : user.getName();
        return toDto(comment, displayName);
    }

    @Transactional
    public CommentResponseDto updateComment(Long userId, Long eventId, Long commentId, UpdateCommentDto dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment id=" + commentId + " not found"));

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Only author can edit comment");
        }
        if (!comment.getEventId().equals(eventId)) {
            throw new NotFoundException("Comment does not belong to event id=" + eventId);
        }

        comment.setText(dto.getText());
        commentRepository.save(comment);

        EventInternalDto event = eventClient.getEventById(eventId);
        UserDto user = userClient.getUserById(userId);
        String displayName = userId.equals(event.getInitiatorId()) ? event.getTitle() : user.getName();

        return toDto(comment, displayName);
    }

    @Transactional
    public void deleteCommentByAuthor(Long userId, Long eventId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment id=" + commentId + " not found"));

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Only author can delete comment");
        }
        if (!comment.getEventId().equals(eventId)) {
            throw new NotFoundException("Comment does not belong to event id=" + eventId);
        }

        commentRepository.deleteById(commentId);
        log.info("Comment id={} deleted by author id={}", commentId, userId);
    }

    public List<CommentResponseDto> getCommentsByEvent(Long eventId, int from, int size) {
        EventInternalDto event = eventClient.getEventById(eventId);
        PageRequest page = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "createdOn"));

        return commentRepository.findAllByEventId(eventId, page).stream()
                .map(c -> {
                    String displayName = c.getAuthorId().equals(event.getInitiatorId())
                            ? event.getTitle()
                            : userClient.getUserById(c.getAuthorId()).getName();
                    return toDto(c, displayName);
                })
                .toList();
    }

    public List<CommentResponseDto> getCommentsByAdmin(String text, Long eventId, Long authorId, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "createdOn"));

        return commentRepository.findCommentsByAdmin(text, eventId, authorId, page).stream()
                .map(c -> {
                    EventInternalDto event = eventClient.getEventById(c.getEventId());
                    String displayName = c.getAuthorId().equals(event.getInitiatorId())
                            ? event.getTitle()
                            : userClient.getUserById(c.getAuthorId()).getName();
                    return toDto(c, displayName);
                })
                .toList();
    }

    @Transactional
    public void deleteCommentByAdmin(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new NotFoundException("Comment id=" + id + " not found");
        }
        commentRepository.deleteById(id);
        log.info("Comment id={} deleted by admin", id);
    }

    private CommentResponseDto toDto(Comment comment, String displayName) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .answerTo(comment.getAnswerTo())
                .name(displayName)
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .build();
    }
}