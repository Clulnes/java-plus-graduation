package ru.practicum.analyzer.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserEventInteraction;

import java.util.List;
import java.util.Optional;

public interface UserEventInteractionRepository extends JpaRepository<UserEventInteraction, Long> {

    Optional<UserEventInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserEventInteraction> findAllByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(i.weight), 0.0) FROM UserEventInteraction i WHERE i.eventId = :eventId")
    Double sumWeightByEventId(@Param("eventId") Long eventId);
}