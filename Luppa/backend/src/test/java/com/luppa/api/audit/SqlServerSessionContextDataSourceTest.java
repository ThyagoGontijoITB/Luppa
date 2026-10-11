package com.luppa.api.audit;

import com.luppa.api.security.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlServerSessionContextDataSourceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticatedUserOnBorrowedConnectionAndClearsBeforeReturningToPool() throws Exception {
        DataSource targetDataSource = mock(DataSource.class);
        Connection targetConnection = mock(Connection.class);
        PreparedStatement setStatement = mock(PreparedStatement.class);
        PreparedStatement clearStatement = mock(PreparedStatement.class);
        AuthUser authUser = mock(AuthUser.class);
        when(authUser.idUsuario()).thenReturn(42);
        when(targetDataSource.getConnection()).thenReturn(targetConnection);
        when(targetConnection.prepareStatement("EXEC sys.sp_set_session_context @key = N'idUsuario', @value = ?"))
                .thenReturn(setStatement, clearStatement);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null, List.of())
        );

        SqlServerSessionContextDataSource dataSource = new SqlServerSessionContextDataSource(
                targetDataSource,
                new AuthenticatedUserIdProvider()
        );
        Connection connection = dataSource.getConnection();
        connection.close();

        verify(setStatement).setInt(1, 42);
        verify(setStatement).execute();
        verify(clearStatement).setNull(1, Types.INTEGER);
        verify(clearStatement).execute();
        verify(targetConnection).close();
    }

    @Test
    void clearsSessionContextForUnauthenticatedConnectionBorrow() throws Exception {
        DataSource targetDataSource = mock(DataSource.class);
        Connection targetConnection = mock(Connection.class);
        PreparedStatement borrowStatement = mock(PreparedStatement.class);
        PreparedStatement closeStatement = mock(PreparedStatement.class);
        when(targetDataSource.getConnection()).thenReturn(targetConnection);
        when(targetConnection.prepareStatement("EXEC sys.sp_set_session_context @key = N'idUsuario', @value = ?"))
                .thenReturn(borrowStatement, closeStatement);

        SqlServerSessionContextDataSource dataSource = new SqlServerSessionContextDataSource(
                targetDataSource,
                new AuthenticatedUserIdProvider()
        );
        Connection connection = dataSource.getConnection();
        connection.close();

        verify(borrowStatement).setNull(1, Types.INTEGER);
        verify(borrowStatement).execute();
        verify(closeStatement).setNull(1, Types.INTEGER);
        verify(closeStatement).execute();
        verify(targetConnection).close();
    }
}
