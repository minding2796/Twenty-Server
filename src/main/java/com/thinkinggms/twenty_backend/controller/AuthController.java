
package com.thinkinggms.twenty_backend.controller;

import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.UserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> getCurrentUser(@AuthenticationPrincipal User user) {
        UserInfoDto userInfo = UserInfoDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .picture(user.getPicture())
                .provider(user.getProvider().name())
                .build();

        return ResponseEntity.ok(userInfo);
    }

    // OAuth2 로그인 성공 후 리다이렉트를 처리하는 엔드포인트
    @GetMapping("/oauth2/redirect")
    public ResponseEntity<Void> oauth2Redirect(@RequestParam("token") String token) {
        return ResponseEntity.status(302)
                .header("Location", "https://twenty.thinkinggms.com/?token=" + token).build();
    }
}