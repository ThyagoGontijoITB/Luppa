package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario")
    private Integer id;

    @Column(name = "nome", nullable = false, length = 35)
    private String firstName;

    @Column(name = "sobrenome", nullable = false, length = 50)
    private String lastName = "";

    @Column(nullable = false, unique = true, length = 60)
    private String email;

    @Column(name = "aniversario", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "numeroTelefone", nullable = false, length = 20)
    private String phone;

    @Column(name = "senha", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idCategoria", nullable = false)
    private Role role;

    @Transient
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "pathImagemPerfil", length = 200)
    private String profileImagePath;

    @Column(name = "emailVerificado", nullable = false)
    private boolean emailVerified;

    @Column(name = "dataVerificacaoEmail")
    private LocalDateTime emailVerifiedAt;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return (firstName + " " + lastName).trim();
    }

    public void setName(String name) {
        String normalized = name == null ? "" : name.trim();
        int split = normalized.indexOf(' ');
        if (split < 0) {
            this.firstName = truncate(normalized, 35);
            this.lastName = "";
        } else {
            this.firstName = truncate(normalized.substring(0, split).trim(), 35);
            this.lastName = truncate(normalized.substring(split + 1).trim(), 50);
        }
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status == null ? UserStatus.ACTIVE : status; }
    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath == null ? "" : truncate(profileImagePath, 200); }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    public java.time.Instant getCreatedAt() { return null; }
    public java.time.Instant getUpdatedAt() { return null; }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
