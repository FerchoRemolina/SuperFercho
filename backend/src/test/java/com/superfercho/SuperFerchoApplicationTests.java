package com.superfercho;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.assistant.application.fercho.FerchoSupervisionPort;
import com.superfercho.assistant.infrastructure.rest.FerchoAdminController;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.infrastructure.configuration.LocalDevAdminRunner;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SuperFerchoApplicationTests {

    @Autowired
    private Clock clock;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(clock).isNotNull();
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(passwordHasher).isNotNull();
        assertThat(currentUserProvider).isNotNull();
        assertThat(securityFilterChain).isNotNull();
        assertThat(applicationContext.getBeanNamesForType(LocalDevAdminRunner.class)).isEmpty();
        // Perfil test: sin DataSource/JPA. El adaptador JPA y el REST admin de Fercho
        // no se registran; las rutas se cubren en WebMvcTest y Testcontainers.
        assertThat(applicationContext.getBeanNamesForType(FerchoAdminController.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(FerchoSupervisionPort.class)).isEmpty();
    }
}
