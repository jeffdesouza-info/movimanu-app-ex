package com.antlia.movimanu.integration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class MovimanuApplicationIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startsWithInMemoryDatabaseAndMigratedSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:movimanu-test");
        }

        Integer productTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'PRODUTO'",
                Integer.class);
        assertThat(productTableCount).isEqualTo(1);

        Integer seededProductCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PRODUTO", Integer.class);
        assertThat(seededProductCount).isZero();
    }
}
