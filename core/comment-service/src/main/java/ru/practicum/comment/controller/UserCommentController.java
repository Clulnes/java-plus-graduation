package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentResponseDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.service.CommentServiceImpl;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/comments")
@RequiredArgsConstructor
public class UserCommentController {

    private final CommentServiceImpl commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto createComment(@PathVariable Long userId,
                                            @PathVariable Long eventId,
                                            @Valid @RequestBody NewCommentDto dto) {
        return commentService.createComment(userId, eventId, dto);
    }

    @PatchMapping("/{commentId}")
    public CommentResponseDto updateComment(@PathVariable Long userId,
                                            @PathVariable Long eventId,
                                            @PathVariable Long commentId,
                                            @Valid @RequestBody UpdateCommentDto dto) {
        return commentService.updateComment(userId, eventId, commentId, dto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long eventId,
                              @PathVariable Long commentId) {
        commentService.deleteCommentByAuthor(userId, eventId, commentId);
    }
}