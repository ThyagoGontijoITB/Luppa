USE luppaBD;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

/*
Rollback scope:
- Restores fk_logAuditoria_usuario to ON DELETE NO ACTION.
- Removes the preservation trigger.
- Does not drop idUsuarioHistorico by default because it may now be the only
  place where deleted users' historical IDs still exist.

Only set @DropHistoricalColumn = 1 if you have confirmed that no preserved
history would be lost. The script validates that before dropping the column.
*/

DECLARE @DropHistoricalColumn bit = 0;

BEGIN TRY
    IF DB_NAME() <> N'luppaBD'
        THROW 51200, 'Wrong database. Rollback must run only in luppaBD.', 1;

    IF OBJECT_ID(N'dbo.logAuditoria', N'U') IS NULL
        THROW 51201, 'Required table dbo.logAuditoria was not found.', 1;

    IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
        THROW 51202, 'Required table dbo.usuario was not found.', 1;

    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.trg_logAuditoria_preserva_idUsuarioHistorico', N'TR') IS NOT NULL
        DROP TRIGGER dbo.trg_logAuditoria_preserva_idUsuarioHistorico;

    IF OBJECT_ID(N'dbo.fk_logAuditoria_usuario', N'F') IS NOT NULL
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM sys.foreign_keys fk
            JOIN sys.foreign_key_columns fkc
              ON fkc.constraint_object_id = fk.object_id
            JOIN sys.columns pc
              ON pc.object_id = fk.parent_object_id
             AND pc.column_id = fkc.parent_column_id
            JOIN sys.columns rc
              ON rc.object_id = fk.referenced_object_id
             AND rc.column_id = fkc.referenced_column_id
            WHERE fk.name = N'fk_logAuditoria_usuario'
              AND fk.parent_object_id = OBJECT_ID(N'dbo.logAuditoria')
              AND fk.referenced_object_id = OBJECT_ID(N'dbo.usuario')
              AND pc.name = N'idUsuario'
              AND rc.name = N'idUsuario'
        )
            THROW 51203, 'Constraint dbo.fk_logAuditoria_usuario exists but does not match logAuditoria(idUsuario) -> usuario(idUsuario).', 1;

        ALTER TABLE dbo.logAuditoria
            DROP CONSTRAINT fk_logAuditoria_usuario;
    END;

    IF OBJECT_ID(N'dbo.fk_logAuditoria_usuario', N'F') IS NULL
    BEGIN
        ALTER TABLE dbo.logAuditoria WITH CHECK
            ADD CONSTRAINT fk_logAuditoria_usuario
            FOREIGN KEY (idUsuario)
            REFERENCES dbo.usuario (idUsuario)
            ON DELETE NO ACTION;
    END;

    ALTER TABLE dbo.logAuditoria WITH CHECK
        CHECK CONSTRAINT fk_logAuditoria_usuario;

    IF @DropHistoricalColumn = 1
    BEGIN
        IF COL_LENGTH(N'dbo.logAuditoria', N'idUsuarioHistorico') IS NOT NULL
        BEGIN
            IF EXISTS (
                SELECT 1
                FROM dbo.logAuditoria
                WHERE idUsuarioHistorico IS NOT NULL
                  AND (idUsuario IS NULL OR idUsuarioHistorico <> idUsuario)
            )
                THROW 51204, 'Cannot drop idUsuarioHistorico without losing preserved historical user IDs.', 1;

            ALTER TABLE dbo.logAuditoria
                DROP COLUMN idUsuarioHistorico;
        END;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.foreign_keys
        WHERE object_id = OBJECT_ID(N'dbo.fk_logAuditoria_usuario', N'F')
          AND delete_referential_action_desc = N'NO_ACTION'
          AND is_disabled = 0
          AND is_not_trusted = 0
    )
        THROW 51205, 'Rollback validation failed: fk_logAuditoria_usuario is not trusted ON DELETE NO ACTION.', 1;

    COMMIT TRANSACTION;

    SELECT
        'OK' AS status,
        'fk_logAuditoria_usuario restored to ON DELETE NO ACTION. idUsuarioHistorico was preserved unless @DropHistoricalColumn was set to 1.' AS message;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
