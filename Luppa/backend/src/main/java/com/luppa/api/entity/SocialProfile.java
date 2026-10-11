package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfilRedesSociais")
public class SocialProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRede")
    private Integer id;

    @Column(name = "plataforma", nullable = false, length = 50)
    private String platform;

    public Integer getId() {
        return id;
    }

    public String getPlatform() {
        return platform;
    }
}
