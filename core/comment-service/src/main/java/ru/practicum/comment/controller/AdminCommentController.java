package ru.practicum.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentResponseDto;
import ru.practicum.comment.service.CommentServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentServiceImpl commentService;

    @GetMapping
    public List<CommentResponseDto> getComments(@RequestParam(required = false) String text,
                                                @RequestParam(required = false) Long eventId,
                                                @RequestParam(required = false) Long authorId,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                @RequestParam(defaultValue = "10") @Positive int size) {
        return commentService.getCommentsByAdmin(text, eventId, authorId, from, size);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteCommentByAdmin(id);
    }
}