package com.antlia.movimanu.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:movimanu-local-seed;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("local")
class MovimanuLocalDataIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsDemonstrationProductsAndCosifsOnlyWhenTheLocalProfileIsActive() {
        Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PRODUTO", Integer.class);
        Integer cosifCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PRODUTO_COSIF", Integer.class);

        assertThat(productCount).isEqualTo(2);
        assertThat(cosifCount).isEqualTo(3);
    }
}
