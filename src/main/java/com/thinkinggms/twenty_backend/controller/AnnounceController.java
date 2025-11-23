package com.thinkinggms.twenty_backend.controller;

import com.thinkinggms.twenty_backend.domain.News;
import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.NewsListResponse;
import com.thinkinggms.twenty_backend.service.AnnounceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announce")
@RequiredArgsConstructor
public class AnnounceController {
    private final AnnounceService announceService;

    @GetMapping("/news-list")
    public ResponseEntity<NewsListResponse> getNewsList() {
        return ResponseEntity.ok(announceService.getNewsList());
    }

    @GetMapping("/news")
    public ResponseEntity<News> getNewsList(@RequestParam long id) {
        return ResponseEntity.ok(announceService.getNews(id));
    }

    @PostMapping("/post-news")
    public ResponseEntity<Void> postNews(@AuthenticationPrincipal User user, @RequestBody News news) {
        if (user.getRole() != User.Role.ADMIN) return ResponseEntity.status(403).build();
        announceService.postNews(news);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete-news")
    public ResponseEntity<Void> deleteNews(@AuthenticationPrincipal User user, @RequestBody News news) {
        if (user.getRole() != User.Role.ADMIN) return ResponseEntity.status(403).build();
        announceService.removeNews(news.getId());
        return ResponseEntity.ok().build();
    }
}
