package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventInternalDto {
    private Long id;
    private String title;
    private Long initiatorId;
    private String state;
    private Integer participantLimit;
    private Integer confirmedRequests;
    private Boolean requestModeration;
}