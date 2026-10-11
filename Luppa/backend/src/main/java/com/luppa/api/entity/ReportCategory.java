package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoriasDenuncia")
public class ReportCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMotivo")
    private Integer id;

    @Column(name = "categoria", nullable = false, length = 50)
    private String category;

    public Integer getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }
}
