USE luppaBD;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    IF DB_NAME() <> N'luppaBD'
        THROW 51000, 'Wrong database. This migration must run only in luppaBD.', 1;

    IF OBJECT_ID(N'dbo.logAuditoria', N'U') IS NULL
        THROW 51001, 'Required table dbo.logAuditoria was not found.', 1;

    IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
        THROW 51002, 'Required table dbo.usuario was not found.', 1;

    DECLARE @nestedTriggers int = (
        SELECT CAST(value_in_use AS int)
        FROM sys.configurations
        WHERE name = N'nested triggers'
    );

    IF COALESCE(@nestedTriggers, 0) <> 1
        THROW 51013, 'SQL Server nested AFTER triggers are disabled. Existing audit triggers insert into logAuditoria from triggers, so idUsuarioHistorico cannot be preserved by this migration until nested triggers are enabled.', 1;

    IF HAS_PERMS_BY_NAME(N'dbo.logAuditoria', N'OBJECT', N'ALTER') <> 1
        THROW 51014, 'Missing ALTER permission on dbo.logAuditoria. Run this migration with the database owner or an approved DDL migration principal.', 1;

    IF HAS_PERMS_BY_NAME(N'dbo.logAuditoria', N'OBJECT', N'UPDATE') <> 1
        THROW 51015, 'Missing UPDATE permission on dbo.logAuditoria. Run this migration with the database owner or an approved DDL migration principal.', 1;

    IF HAS_PERMS_BY_NAME(N'dbo.usuario', N'OBJECT', N'REFERENCES') <> 1
        THROW 51016, 'Missing REFERENCES permission on dbo.usuario. Run this migration with the database owner or an approved DDL migration principal.', 1;

    BEGIN TRANSACTION;

    DECLARE @auditRowsBefore bigint =
        (SELECT COUNT_BIG(*) FROM dbo.logAuditoria WITH (HOLDLOCK));

    IF NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints kc
        JOIN sys.index_columns ic
          ON ic.object_id = kc.parent_object_id
         AND ic.index_id = kc.unique_index_id
        JOIN sys.columns c
          ON c.object_id = ic.object_id
         AND c.column_id = ic.column_id
        WHERE kc.parent_object_id = OBJECT_ID(N'dbo.usuario')
          AND kc.type = 'PK'
          AND c.name = N'idUsuario'
    )
        THROW 51003, 'dbo.usuario primary key is not idUsuario. Review the schema before applying this migration.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints kc
        JOIN sys.index_columns ic
          ON ic.object_id = kc.parent_object_id
         AND ic.index_id = kc.unique_index_id
        JOIN sys.columns c
          ON c.object_id = ic.object_id
         AND c.column_id = ic.column_id
        WHERE kc.parent_object_id = OBJECT_ID(N'dbo.logAuditoria')
          AND kc.type = 'PK'
          AND c.name = N'idLog'
    )
        THROW 51004, 'dbo.logAuditoria primary key is not idLog. Review the schema before applying this migration.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.logAuditoria')
          AND name = N'idUsuario'
          AND system_type_id = TYPE_ID(N'int')
          AND is_nullable = 1
    )
        THROW 51005, 'dbo.logAuditoria.idUsuario must be nullable int before this migration can run.', 1;

    IF COL_LENGTH(N'dbo.logAuditoria', N'idUsuarioHistorico') IS NULL
    BEGIN
        ALTER TABLE dbo.logAuditoria
            ADD idUsuarioHistorico int NULL;
    END;
    ELSE IF NOT EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.logAuditoria')
          AND name = N'idUsuarioHistorico'
          AND system_type_id = TYPE_ID(N'int')
          AND is_nullable = 1
    )
        THROW 51006, 'dbo.logAuditoria.idUsuarioHistorico exists but is not nullable int.', 1;

    UPDATE dbo.logAuditoria
       SET idUsuarioHistorico = idUsuario
     WHERE idUsuarioHistorico IS NULL
       AND idUsuario IS NOT NULL;

    IF EXISTS (
        SELECT 1
        FROM dbo.logAuditoria
        WHERE idUsuario IS NOT NULL
          AND idUsuarioHistorico IS NULL
    )
        THROW 51007, 'Backfill validation failed: some non-null idUsuario values were not preserved.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.logAuditoria
        WHERE idUsuario IS NOT NULL
          AND idUsuarioHistorico IS NOT NULL
          AND idUsuarioHistorico <> idUsuario
    )
        THROW 51017, 'Existing idUsuarioHistorico values conflict with current idUsuario values. Review the partially migrated data before applying this migration.', 1;

    DECLARE @fkId int = OBJECT_ID(N'dbo.fk_logAuditoria_usuario', N'F');

    IF @fkId IS NOT NULL
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
            WHERE fk.object_id = @fkId
              AND fk.parent_object_id = OBJECT_ID(N'dbo.logAuditoria')
              AND fk.referenced_object_id = OBJECT_ID(N'dbo.usuario')
              AND pc.name = N'idUsuario'
              AND rc.name = N'idUsuario'
        )
            THROW 51008, 'Constraint dbo.fk_logAuditoria_usuario exists but does not match logAuditoria(idUsuario) -> usuario(idUsuario).', 1;

        IF EXISTS (
            SELECT 1
            FROM sys.foreign_keys
            WHERE object_id = @fkId
              AND delete_referential_action_desc <> N'SET_NULL'
        )
        BEGIN
            ALTER TABLE dbo.logAuditoria
                DROP CONSTRAINT fk_logAuditoria_usuario;
        END;
    END;

    IF OBJECT_ID(N'dbo.fk_logAuditoria_usuario', N'F') IS NULL
    BEGIN
        IF EXISTS (
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
            WHERE fk.parent_object_id = OBJECT_ID(N'dbo.logAuditoria')
              AND fk.referenced_object_id = OBJECT_ID(N'dbo.usuario')
              AND pc.name = N'idUsuario'
              AND rc.name = N'idUsuario'
              AND fk.name <> N'fk_logAuditoria_usuario'
        )
            THROW 51009, 'Another FK already links logAuditoria(idUsuario) to usuario(idUsuario). Review it before creating fk_logAuditoria_usuario.', 1;

        ALTER TABLE dbo.logAuditoria WITH CHECK
            ADD CONSTRAINT fk_logAuditoria_usuario
            FOREIGN KEY (idUsuario)
            REFERENCES dbo.usuario (idUsuario)
            ON DELETE SET NULL;
    END;

    ALTER TABLE dbo.logAuditoria WITH CHECK
        CHECK CONSTRAINT fk_logAuditoria_usuario;

    EXEC(N'
CREATE OR ALTER TRIGGER dbo.trg_logAuditoria_preserva_idUsuarioHistorico
ON dbo.logAuditoria
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE la
       SET idUsuarioHistorico = i.idUsuario
    FROM dbo.logAuditoria AS la
    INNER JOIN inserted AS i
        ON i.idLog = la.idLog
    WHERE la.idUsuarioHistorico IS NULL
      AND i.idUsuario IS NOT NULL;
END;
');

    DECLARE @auditRowsAfter bigint =
        (SELECT COUNT_BIG(*) FROM dbo.logAuditoria);

    IF @auditRowsAfter <> @auditRowsBefore
        THROW 51010, 'Audit row count changed during migration. Rolling back.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.foreign_keys
        WHERE object_id = OBJECT_ID(N'dbo.fk_logAuditoria_usuario', N'F')
          AND delete_referential_action_desc = N'SET_NULL'
          AND is_disabled = 0
          AND is_not_trusted = 0
    )
        THROW 51011, 'FK validation failed: fk_logAuditoria_usuario is not trusted ON DELETE SET NULL.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.triggers
        WHERE object_id = OBJECT_ID(N'dbo.trg_logAuditoria_preserva_idUsuarioHistorico', N'TR')
          AND parent_id = OBJECT_ID(N'dbo.logAuditoria')
          AND is_disabled = 0
    )
        THROW 51012, 'Trigger validation failed: dbo.trg_logAuditoria_preserva_idUsuarioHistorico is missing or disabled.', 1;

    COMMIT TRANSACTION;

    SELECT
        'OK' AS status,
        @auditRowsAfter AS auditRowsPreserved,
        'dbo.logAuditoria.idUsuarioHistorico is populated and fk_logAuditoria_usuario now uses ON DELETE SET NULL.' AS message;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
