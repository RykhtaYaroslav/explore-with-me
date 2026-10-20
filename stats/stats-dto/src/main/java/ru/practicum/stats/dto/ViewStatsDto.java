package ru.practicum.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("unused") // чтобы IDEA не ругалась, что этот класс никогда не используется (жёлтая ошибка)
public class ViewStatsDto {
    private String app;
    private String uri;
    private Long hits;
}
