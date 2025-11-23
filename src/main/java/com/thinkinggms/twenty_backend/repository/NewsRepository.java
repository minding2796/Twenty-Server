package com.thinkinggms.twenty_backend.repository;

import com.thinkinggms.twenty_backend.domain.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findAllByOrderByIdDesc();
    News findByTitle(String title);
}
