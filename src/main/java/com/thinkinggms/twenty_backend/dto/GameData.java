package com.thinkinggms.twenty_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class GameData {
    private String userEmail;
    private Integer currentFunds;
    private Integer goStack;
    private Integer cooldown;
    private Integer stockPrice;
    private List<Integer> priceHistory;
    private Integer colCooldown;
}
