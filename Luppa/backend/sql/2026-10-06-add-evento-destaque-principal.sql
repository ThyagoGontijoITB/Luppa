IF COL_LENGTH('dbo.evento', 'destaquePrincipal') IS NULL
BEGIN
    ALTER TABLE dbo.evento
        ADD destaquePrincipal BIT NOT NULL
            CONSTRAINT DF_evento_destaquePrincipal DEFAULT 0;
END;
