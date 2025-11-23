package com.thinkinggms.twenty_backend.service;

import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.UserInfoDto;
import com.thinkinggms.twenty_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    public void editUser(User user, UserInfoDto newUser) {
        userRepository.save(user.update(newUser.getName()));
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        User.AuthProvider provider = getProvider(registrationId);
        UserProfile userProfile = extractUserProfile(provider, attributes);

        User user = saveOrUpdateUser(provider, userProfile);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                attributes,
                userNameAttributeName);
    }

    private User.AuthProvider getProvider(String registrationId) {
        if (registrationId.equals("google")) {
            return User.AuthProvider.GOOGLE;
        } else if (registrationId.equals("discord")) {
            return User.AuthProvider.DISCORD;
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }
    }

    private UserProfile extractUserProfile(User.AuthProvider provider, Map<String, Object> attributes) {
        if (provider == User.AuthProvider.GOOGLE) {
            return UserProfile.builder()
                    .id(attributes.get("sub").toString())
                    .name(attributes.get("name").toString())
                    .email(attributes.get("email").toString())
                    .picture(attributes.get("picture").toString())
                    .build();
        } else if (provider == User.AuthProvider.DISCORD) {
            String id = attributes.get("id").toString();
            String name = attributes.get("username").toString();
            String email = attributes.containsKey("email") ? attributes.get("email").toString() : id + "@discord.com";

            Object a = attributes.get("avatar");
            String avatarId = a != null ? a.toString() : null;
            String picture = null;
            if (avatarId != null) picture = "https://cdn.discordapp.com/avatars/" + id + "/" + avatarId + ".png";
            if (picture == null) picture = "https://cdn.discordapp.com/embed/avatars/" + (Integer.parseInt(id) % 5) + ".png";

            return UserProfile.builder()
                    .id(id)
                    .name(name)
                    .email(email)
                    .picture(picture)
                    .build();
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + provider);
        }
    }

    private User saveOrUpdateUser(User.AuthProvider provider, UserProfile profile) {
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(provider, profile.getId());

        if (userOptional.isPresent()) {
            // 기존 사용자 업데이트
            User existingUser = userOptional.get();
            return existingUser.update(profile.getName());
        } else {
            // 새 사용자 등록
            User user = User.builder()
                    .name(profile.getName())
                    .email(profile.getEmail())
                    .picture(profile.getPicture())
                    .provider(provider)
                    .providerId(profile.getId())
                    .role(User.Role.USER)
                    .build();
            return userRepository.save(user);
        }
    }

    @lombok.Builder
    @lombok.Getter
    private static class UserProfile {
        private String id;
        private String name;
        private String email;
        private String picture;
    }
}