package com.thinkinggms.twenty_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserInfoDto {
    private Long id;
    private String name;
    private String email;
    private String picture;
    private String provider;
}