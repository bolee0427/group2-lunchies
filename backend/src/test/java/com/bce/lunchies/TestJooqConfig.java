package com.bce.lunchies;

import com.bce.lunchies.repository.Tables;
import io.r2dbc.spi.ConnectionFactory;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestJooqConfig {

    @Bean
    public DSLContext dslContext(ConnectionFactory connectionFactory) {
        return DSL.using(connectionFactory, SQLDialect.POSTGRES);
    }

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__init.sql")));
        return initializer;
    }

    public static void cleanAllTables(DSLContext dsl) {
        Mono.from(dsl.deleteFrom(Tables.Attendance.TABLE)).block();
        Mono.from(dsl.deleteFrom(Tables.MenuItem.TABLE)).block();
        Mono.from(dsl.deleteFrom(Tables.Menu.TABLE)).block();
        Mono.from(dsl.deleteFrom(Tables.AppUser.TABLE)).block();
    }
}
