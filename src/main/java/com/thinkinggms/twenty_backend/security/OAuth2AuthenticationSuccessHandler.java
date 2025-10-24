package com.thinkinggms.twenty_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Value("${app.oauth2.redirectUri:/api/auth/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(authentication);

        if (response.isCommitted()) {
            logger.debug("응답이 이미 커밋되어 리다이렉트할 수 없습니다: " + targetUrl);
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(Authentication authentication) {
        String token = tokenProvider.generateToken(authentication);

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();
    }
}