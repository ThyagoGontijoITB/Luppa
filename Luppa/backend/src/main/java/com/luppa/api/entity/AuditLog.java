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
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "logAuditoria")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idLog")
    private Long id;

    @Column(name = "tabela", nullable = false, length = 100)
    private String tableName;

    @Column(name = "operacao", nullable = false, length = 10)
    private String operation;

    @Column(name = "idRegistro")
    private Integer recordId;

    @Column(name = "idUsuario")
    private Integer userId;

    @Column(name = "idUsuarioHistorico")
    private Integer historicalUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuario", insertable = false, updatable = false)
    private User user;

    @Column(name = "dadosAntigos", columnDefinition = "varchar(max)")
    private String oldData;

    @Column(name = "dadosNovos", columnDefinition = "varchar(max)")
    private String newData;

    @Column(name = "dataOperacao", nullable = false)
    private LocalDateTime operatedAt;

    public Long getId() {
        return id;
    }

    public String getTableName() {
        return tableName;
    }

    public String getOperation() {
        return operation;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getHistoricalUserId() {
        return historicalUserId;
    }

    public User getUser() {
        return user;
    }

    public String getOldData() {
        return oldData;
    }

    public String getNewData() {
        return newData;
    }

    public LocalDateTime getOperatedAt() {
        return operatedAt;
    }
}
