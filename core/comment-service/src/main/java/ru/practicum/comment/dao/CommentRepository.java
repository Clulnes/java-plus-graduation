package ru.practicum.comment.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.comment.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByEventId(Long eventId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE " +
            "(:text IS NULL OR LOWER(c.text) LIKE LOWER(CONCAT('%', :text, '%'))) AND " +
            "(:eventId IS NULL OR c.eventId = :eventId) AND " +
            "(:authorId IS NULL OR c.authorId = :authorId)")
    Page<Comment> findCommentsByAdmin(@Param("text") String text,
                                      @Param("eventId") Long eventId,
                                      @Param("authorId") Long authorId,
                                      Pageable pageable);
}