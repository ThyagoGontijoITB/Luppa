USE luppaBD;
GO

IF EXISTS (
    SELECT 1
    FROM dbo.tipoUsuario
    WHERE idCategoria = 3
      AND descricaoCategoria <> 'Administrador principal da Luppa.'
)
BEGIN
    THROW 50001, 'idCategoria 3 is already in use by a different tipoUsuario row. Review the schema before applying LUPPA_ADMIN.', 1;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM dbo.tipoUsuario
    WHERE idCategoria = 3
)
BEGIN
    SET IDENTITY_INSERT dbo.tipoUsuario ON;

    INSERT INTO dbo.tipoUsuario (idCategoria, descricaoCategoria)
    VALUES (3, 'Administrador principal da Luppa.');

    SET IDENTITY_INSERT dbo.tipoUsuario OFF;
END;
GO
