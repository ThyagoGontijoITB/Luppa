/*
Run this script manually in SSMS against the remote db72445 database after
importing the generated admin account script.

This script is read-only and intentionally does not select password hashes,
tokens, credentials, or secrets.
*/

USE [db72445];
GO

SET NOCOUNT ON;

IF DB_NAME() <> N'db72445'
    THROW 52200, 'Run this verification only against the remote db72445 database.', 1;

DECLARE @IdUsuario int = 24;

IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
    THROW 52201, 'Required table dbo.usuario was not found.', 1;

IF OBJECT_ID(N'dbo.tipoUsuario', N'U') IS NULL
    THROW 52202, 'Required table dbo.tipoUsuario was not found.', 1;

IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE idUsuario = @IdUsuario)
    THROW 52203, 'Remote user idUsuario=24 was not found.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.usuario u
    JOIN dbo.tipoUsuario tu
      ON tu.idCategoria = u.idCategoria
    WHERE u.idUsuario = @IdUsuario
      AND u.idCategoria IN (0, 3)
)
    THROW 52204, 'Remote user idUsuario=24 does not have an administrator role idCategoria.', 1;

SELECT
    DB_NAME() AS database_name,
    u.idUsuario,
    u.nome,
    u.sobrenome,
    u.email,
    u.idCategoria,
    tu.descricaoCategoria,
    u.emailVerificado,
    u.dataVerificacaoEmail,
    CASE
        WHEN u.senha IS NULL OR LEN(u.senha) <= 20 THEN 'invalid'
        ELSE 'present'
    END AS password_hash_status
FROM dbo.usuario u
JOIN dbo.tipoUsuario tu
  ON tu.idCategoria = u.idCategoria
WHERE u.idUsuario = @IdUsuario;

IF OBJECT_ID(N'dbo.preferenciaComunicacaoUsuario', N'U') IS NOT NULL
BEGIN
    SELECT
        idUsuario,
        receberNovidadesLuppa,
        receberRecomendacoesEvento,
        receberLembretesEventoSalvo,
        dataAtualizacao
    FROM dbo.preferenciaComunicacaoUsuario
    WHERE idUsuario = @IdUsuario;
END;

IF OBJECT_ID(N'dbo.consentimentoMarketingUsuario', N'U') IS NOT NULL
BEGIN
    SELECT
        idUsuario,
        aceitouMarketing,
        dataConsentimento,
        dataRevogacao,
        dataAtualizacao
    FROM dbo.consentimentoMarketingUsuario
    WHERE idUsuario = @IdUsuario;
END;

IF OBJECT_ID(N'dbo.aceiteLegalUsuario', N'U') IS NOT NULL
BEGIN
    SELECT
        idUsuario,
        tipoDocumento,
        versaoDocumento,
        dataAceite
    FROM dbo.aceiteLegalUsuario
    WHERE idUsuario = @IdUsuario
    ORDER BY dataAceite, tipoDocumento, versaoDocumento;
END;
