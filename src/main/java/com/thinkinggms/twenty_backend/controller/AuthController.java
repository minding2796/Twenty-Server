
package com.thinkinggms.twenty_backend.controller;

import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.UserInfoDto;
import com.thinkinggms.twenty_backend.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CustomOAuth2UserService customOAuth2UserService;

    @PostMapping("/edit")
    public ResponseEntity<UserInfoDto> editUser(@AuthenticationPrincipal User user, @RequestBody UserInfoDto newUser) {
        customOAuth2UserService.editUser(user, newUser);

        return ResponseEntity.ok(newUser);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(403).build();
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
    public ResponseEntity<?> oauth2Redirect(@RequestParam("token") String token) {
        String redirectUrl = "https://twenty.thinkinggms.com/?token=" + token;
        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="refresh" content="0;url=%s">
                    <script>window.location.href="%s";</script>
                </head>
                <body>
                    Redirecting to <a href="%s">Twenty</a>...
                </body>
                </html>
                """.formatted(redirectUrl, redirectUrl, redirectUrl);

        return ResponseEntity.ok()
                .header("Location", redirectUrl)
                .header("Content-Type", "text/html;charset=UTF-8")
                .body(htmlContent);
    }
}