USE luppaBD;
GO

SET NOCOUNT ON;

IF DB_NAME() <> N'luppaBD'
    THROW 51100, 'Wrong database. Verification must run only in luppaBD.', 1;

DECLARE @failures table (
    check_name varchar(120) NOT NULL,
    details varchar(500) NOT NULL
);

IF NOT EXISTS (
    SELECT 1
    FROM sys.configurations
    WHERE name = N'nested triggers'
      AND CAST(value_in_use AS int) = 1
)
    INSERT INTO @failures VALUES
        ('nested-triggers', 'SQL Server nested AFTER triggers are disabled; audit inserts made from existing audit triggers will not fire the preservation trigger.');

IF COL_LENGTH(N'dbo.logAuditoria', N'idUsuarioHistorico') IS NULL
    INSERT INTO @failures VALUES
        ('historical-column', 'dbo.logAuditoria.idUsuarioHistorico does not exist.');
ELSE IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.logAuditoria')
      AND name = N'idUsuarioHistorico'
      AND system_type_id = TYPE_ID(N'int')
      AND is_nullable = 1
)
    INSERT INTO @failures VALUES
        ('historical-column', 'dbo.logAuditoria.idUsuarioHistorico exists but is not nullable int.');

IF EXISTS (
    SELECT 1
    FROM dbo.logAuditoria
    WHERE idUsuario IS NOT NULL
      AND idUsuarioHistorico IS NULL
)
    INSERT INTO @failures VALUES
        ('backfill', 'At least one audit row has idUsuario but no idUsuarioHistorico.');

IF EXISTS (
    SELECT 1
    FROM dbo.logAuditoria
    WHERE idUsuario IS NOT NULL
      AND idUsuarioHistorico IS NOT NULL
      AND idUsuarioHistorico <> idUsuario
)
    INSERT INTO @failures VALUES
        ('backfill', 'At least one audit row has conflicting idUsuario and idUsuarioHistorico values.');

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
      AND fk.delete_referential_action_desc = N'SET_NULL'
      AND fk.is_disabled = 0
      AND fk.is_not_trusted = 0
)
    INSERT INTO @failures VALUES
        ('foreign-key', 'fk_logAuditoria_usuario is not trusted ON DELETE SET NULL.');

IF NOT EXISTS (
    SELECT 1
    FROM sys.triggers
    WHERE object_id = OBJECT_ID(N'dbo.trg_logAuditoria_preserva_idUsuarioHistorico', N'TR')
      AND parent_id = OBJECT_ID(N'dbo.logAuditoria')
      AND is_disabled = 0
)
    INSERT INTO @failures VALUES
        ('preserve-trigger', 'dbo.trg_logAuditoria_preserva_idUsuarioHistorico is missing or disabled.');

IF EXISTS (
    SELECT 1
    FROM sys.triggers tr
    JOIN sys.objects o
      ON o.object_id = tr.object_id
    WHERE tr.parent_id = OBJECT_ID(N'dbo.logAuditoria')
      AND o.name = N'trg_logAuditoria_preserva_idUsuarioHistorico'
      AND (OBJECT_DEFINITION(tr.object_id) IS NULL
           OR OBJECT_DEFINITION(tr.object_id) NOT LIKE N'%idUsuarioHistorico%'
           OR OBJECT_DEFINITION(tr.object_id) NOT LIKE N'%inserted%')
)
    INSERT INTO @failures VALUES
        ('preserve-trigger', 'The preservation trigger definition does not copy inserted idUsuario to idUsuarioHistorico.');

DECLARE @auditTriggerCount int = (
    SELECT COUNT(*)
    FROM sys.triggers tr
    JOIN sys.objects o
      ON o.object_id = tr.object_id
    WHERE o.name LIKE N'trg_auditoria[_]%'
      AND tr.is_disabled = 0
);

DECLARE @auditTriggersUsingSessionContext int = (
    SELECT COUNT(*)
    FROM sys.triggers tr
    JOIN sys.objects o
      ON o.object_id = tr.object_id
    WHERE o.name LIKE N'trg_auditoria[_]%'
      AND tr.is_disabled = 0
      AND OBJECT_DEFINITION(tr.object_id) LIKE N'%SESSION_CONTEXT(N''idUsuario'')%'
      AND OBJECT_DEFINITION(tr.object_id) LIKE N'%INSERT INTO logAuditoria%'
);

IF @auditTriggerCount = 0
    INSERT INTO @failures VALUES
        ('audit-triggers', 'No enabled trg_auditoria_* triggers were found.');
ELSE IF @auditTriggersUsingSessionContext <> @auditTriggerCount
    INSERT INTO @failures VALUES
        ('session-context', 'At least one enabled audit trigger is not visible or does not reference SESSION_CONTEXT(N''idUsuario'') and logAuditoria. Run as dbo or inspect trigger definitions.');

IF EXISTS (SELECT 1 FROM @failures)
BEGIN
    SELECT check_name, details
    FROM @failures
    ORDER BY check_name;

    THROW 51101, 'Audit user history verification failed. See result set for details.', 1;
END;

SELECT
    'OK' AS status,
    (SELECT COUNT_BIG(*) FROM dbo.logAuditoria) AS auditRows,
    (SELECT COUNT_BIG(*) FROM dbo.logAuditoria WHERE idUsuarioHistorico IS NOT NULL) AS rowsWithHistoricalUserId,
    (SELECT COUNT_BIG(*) FROM dbo.logAuditoria WHERE idUsuario IS NULL AND idUsuarioHistorico IS NOT NULL) AS rowsPreservedAfterUserDelete,
    @auditTriggerCount AS enabledAuditTriggers,
    @auditTriggersUsingSessionContext AS auditTriggersUsingSessionContext;

SELECT
    fk.name AS fk_name,
    fk.delete_referential_action_desc,
    fk.is_disabled,
    fk.is_not_trusted
FROM sys.foreign_keys fk
WHERE fk.name = N'fk_logAuditoria_usuario';

SELECT
    o.name AS trigger_name,
    OBJECT_NAME(tr.parent_id) AS parent_table,
    tr.is_disabled
FROM sys.triggers tr
JOIN sys.objects o
  ON o.object_id = tr.object_id
WHERE o.name = N'trg_logAuditoria_preserva_idUsuarioHistorico'
   OR o.name LIKE N'trg_auditoria[_]%'
ORDER BY o.name;
