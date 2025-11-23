package com.thinkinggms.twenty_backend.dto;

import com.thinkinggms.twenty_backend.domain.News;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsListResponse {
    private List<News> newsList;
}
