package com.antlia.movimanu.unit;

import com.antlia.movimanu.MovimanuApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class MovimanuApplicationTest {

    @Test
    void declaresSpringBootApplicationEntryPoint() {
        assertThat(MovimanuApplication.class).hasAnnotation(SpringBootApplication.class);
    }
}
