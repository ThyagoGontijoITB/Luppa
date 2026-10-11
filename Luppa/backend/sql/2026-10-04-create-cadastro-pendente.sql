USE luppaBD;
GO

IF OBJECT_ID('dbo.cadastroPendente', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cadastroPendente (
        idCadastroPendente bigint IDENTITY(1,1) NOT NULL,
        nomeCompleto varchar(85) NOT NULL,
        email varchar(60) NOT NULL,
        aniversario date NOT NULL,
        numeroTelefone varchar(20) NOT NULL,
        senhaHash varchar(255) NOT NULL,
        idCategoria int NOT NULL,
        aceitouMarketing bit NOT NULL,
        hashCodigo varchar(255) NOT NULL,
        dataExpiracao datetime2 NOT NULL,
        tentativasFalhas int NOT NULL CONSTRAINT df_cadastroPendente_tentativasFalhas DEFAULT 0,
        dataUltimoEnvio datetime2 NOT NULL,
        dataCriacao datetime2 NOT NULL CONSTRAINT df_cadastroPendente_dataCriacao DEFAULT sysdatetime(),
        dataAtualizacao datetime2 NOT NULL CONSTRAINT df_cadastroPendente_dataAtualizacao DEFAULT sysdatetime(),
        CONSTRAINT pk_cadastroPendente PRIMARY KEY (idCadastroPendente),
        CONSTRAINT uq_cadastroPendente_email UNIQUE (email),
        CONSTRAINT fk_cadastroPendente_tipoUsuario FOREIGN KEY (idCategoria) REFERENCES dbo.tipoUsuario(idCategoria),
        CONSTRAINT ck_cadastroPendente_tentativasFalhas CHECK (tentativasFalhas >= 0),
        CONSTRAINT ck_cadastroPendente_hashCodigo CHECK (LEN(hashCodigo) > 20),
        CONSTRAINT ck_cadastroPendente_senhaHash CHECK (LEN(senhaHash) > 20)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'ix_cadastroPendente_dataExpiracao'
      AND object_id = OBJECT_ID('dbo.cadastroPendente')
)
BEGIN
    CREATE INDEX ix_cadastroPendente_dataExpiracao ON dbo.cadastroPendente(dataExpiracao);
END;
GO
