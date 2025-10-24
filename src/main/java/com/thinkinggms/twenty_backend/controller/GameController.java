package com.thinkinggms.twenty_backend.controller;

import com.thinkinggms.twenty_backend.statics.MatchRoom;
import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.StockResponse;
import com.thinkinggms.twenty_backend.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @GetMapping("/player-status")
    public ResponseEntity<List<MatchRoom.PlayerStatus>> getPlayerStatus(@AuthenticationPrincipal User user) {
        List<MatchRoom.PlayerStatus> list = gameService.getPlayerStatus(user);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/stock-status")
    public ResponseEntity<StockResponse> getStockStatus(@AuthenticationPrincipal User user) {
        StockResponse response = gameService.getStockStatus(user);
        return ResponseEntity.ok(response);
    }
}
