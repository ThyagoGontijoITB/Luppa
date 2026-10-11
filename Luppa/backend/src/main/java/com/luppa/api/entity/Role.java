package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipoUsuario")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCategoria")
    private Integer id;

    @Column(name = "descricaoCategoria", nullable = false, length = 80)
    private String description;

    public Integer getId() {
        return id;
    }

    public UserRole getName() {
        if (id == 0) return UserRole.ADMIN;
        if (id == 1) return UserRole.CREATOR;
        if (id == 2) return UserRole.USER;
        if (id == 3) return UserRole.LUPPA_ADMIN;
        throw new IllegalStateException("Unknown tipoUsuario idCategoria: " + id);
    }

    public void setName(UserRole name) {
        this.id = idFor(name);
        this.description = descriptionFor(name);
    }

    public String getDescription() {
        return description;
    }

    public static int idFor(UserRole role) {
        if (role == UserRole.ADMIN) return 0;
        if (role == UserRole.CREATOR) return 1;
        if (role == UserRole.USER) return 2;
        if (role == UserRole.LUPPA_ADMIN) return 3;
        throw new IllegalStateException("Unknown UserRole: " + role);
    }

    private static String descriptionFor(UserRole role) {
        if (role == UserRole.ADMIN) return "Administrador do sistema.";
        if (role == UserRole.CREATOR) return "Criador, posta conteudos na plataforma.";
        if (role == UserRole.USER) return "Usuario padrão, apenas consome conteudo da plataforma";
        if (role == UserRole.LUPPA_ADMIN) return "Administrador principal da Luppa.";
        throw new IllegalStateException("Unknown UserRole: " + role);
    }
}
