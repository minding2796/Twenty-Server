package com.thinkinggms.twenty_backend.dto;

import com.thinkinggms.twenty_backend.statics.MatchRoom;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponse {
    private Integer stockPrice;
    private List<Float> priceHistory;
    private Integer colCooldown;
    private Integer costOfLiving;

    public static StockResponse from(MatchRoom room) {
        return StockResponse.builder()
                .stockPrice(room.getStockPrice())
                .priceHistory(room.getPriceHistory())
                .colCooldown(room.getColCooldown())
                .costOfLiving(room.getCostOfLiving())
                .build();
    }
}
