package com.thinkinggms.twenty_backend.service;

import com.thinkinggms.twenty_backend.domain.News;
import com.thinkinggms.twenty_backend.dto.NewsListResponse;
import com.thinkinggms.twenty_backend.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnounceService {
    private final NewsRepository newsRepository;

    public NewsListResponse getNewsList() {
        return NewsListResponse.builder().newsList(newsRepository.findAllByOrderByIdDesc()).build();
    }

    public News getNews(long id) {
        return newsRepository.findById(id).orElse(null);
    }

    public void postNews(News news) {
        newsRepository.save(news);
    }

    public void removeNews(long id) {
        newsRepository.deleteById(id);
    }
}
