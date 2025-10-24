package com.thinkinggms.twenty_backend.dto;

import com.thinkinggms.twenty_backend.statics.MatchRoom;
import lombok.*;

import java.util.LinkedList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private String roomCode;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private MatchRoom.RoomStatus status;
    private Integer stockPrice;
    private LinkedList<Integer> priceHistory;

    public static RoomResponse from(MatchRoom room) {
        return RoomResponse.builder()
                .roomCode(room.getRoomCode())
                .maxPlayers(room.getMaxPlayers())
                .currentPlayers(room.getCurrentPlayers())
                .status(room.getStatus())
                .stockPrice(room.getStockPrice())
                .priceHistory(room.getPriceHistory())
                .build();
    }
}