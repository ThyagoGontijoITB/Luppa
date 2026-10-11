package com.luppa.api.audit;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

public class SqlServerSessionContextDataSource extends DelegatingDataSource {
    private static final String SET_ID_USUARIO = "EXEC sys.sp_set_session_context @key = N'idUsuario', @value = ?";

    private final AuthenticatedUserIdProvider userIdProvider;

    public SqlServerSessionContextDataSource(DataSource targetDataSource, AuthenticatedUserIdProvider userIdProvider) {
        super(targetDataSource);
        this.userIdProvider = userIdProvider;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return withAuditContext(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return withAuditContext(super.getConnection(username, password));
    }

    private Connection withAuditContext(Connection connection) throws SQLException {
        setSessionUser(connection, userIdProvider.currentUserId());
        return proxied(connection);
    }

    private Connection proxied(Connection connection) {
        InvocationHandler handler = new AuditConnectionInvocationHandler(connection);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private void clearSessionUser(Connection connection) throws SQLException {
        setSessionUser(connection, Optional.empty());
    }

    private void setSessionUser(Connection connection, Optional<Integer> idUsuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SET_ID_USUARIO)) {
            if (idUsuario.isPresent()) {
                statement.setInt(1, idUsuario.get());
            } else {
                statement.setNull(1, Types.INTEGER);
            }
            statement.execute();
        }
    }

    private final class AuditConnectionInvocationHandler implements InvocationHandler {
        private final Connection target;
        private boolean closed;

        private AuditConnectionInvocationHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if ("close".equals(methodName) && method.getParameterCount() == 0) {
                if (!closed) {
                    closed = true;
                    try {
                        clearSessionUser(target);
                    } finally {
                        target.close();
                    }
                }
                return null;
            }

            if ("unwrap".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> type) {
                if (type.isInstance(proxy)) {
                    return proxy;
                }
                return target.unwrap(type);
            }

            if ("isWrapperFor".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> type) {
                return type.isInstance(proxy) || target.isWrapperFor(type);
            }

            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException exception) {
                throw exception.getTargetException();
            }
        }
    }
}
