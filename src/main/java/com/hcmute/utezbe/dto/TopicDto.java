package com.hcmute.utezbe.dto;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDto {
    private String content;
    private Long forumId;
    private Long accountId;
}
