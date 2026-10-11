package com.luppa.api.audit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class SqlServerSessionContextDataSourcePostProcessor implements BeanPostProcessor {
    private final ObjectProvider<AuthenticatedUserIdProvider> userIdProvider;

    public SqlServerSessionContextDataSourcePostProcessor(ObjectProvider<AuthenticatedUserIdProvider> userIdProvider) {
        this.userIdProvider = userIdProvider;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !(bean instanceof SqlServerSessionContextDataSource)) {
            return new SqlServerSessionContextDataSource(dataSource, userIdProvider.getObject());
        }
        return bean;
    }
}
