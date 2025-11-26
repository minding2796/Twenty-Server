package com.thinkinggms.twenty_backend.domain;

import com.thinkinggms.twenty_backend.dto.GameData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String picture;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Embedded
    private GameData gameData;

    public enum Role {
        USER, ADMIN
    }

    public enum AuthProvider {
        GOOGLE, DISCORD
    }

    public User update(String name) {
        this.name = name;
        return this;
    }

    public User update(GameData data) {
        this.gameData = data;
        return this;
    }
}