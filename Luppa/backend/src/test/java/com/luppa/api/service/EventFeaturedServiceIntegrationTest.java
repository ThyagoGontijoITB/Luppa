package com.luppa.api.service;

import com.luppa.api.dto.EventResponse;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import com.luppa.api.support.RequiresIsolatedSqlServerTestDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@Transactional
@ExtendWith(RequiresIsolatedSqlServerTestDatabase.class)
class EventFeaturedServiceIntegrationTest {
    @Autowired
    private EventService eventService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AccountEmailService accountEmailService;

    @Test
    void updateFeaturedReturnsFullyMappedResponseAndKeepsOnlyOneManualFeaturedEvent() {
        String suffix = Long.toString(System.nanoTime());
        String adminEmail = "admin-" + suffix + "@luppa.test";
        insertUser(adminEmail, 0);
        AuthUser admin = new AuthUser(userRepository.findByEmailIgnoreCase(adminEmail).orElseThrow());
        Integer creatorId = insertUser("creator-" + suffix + "@luppa.test", 1);
        Integer firstEventId = insertEvent(creatorId, "Evento A " + suffix, 3, false);
        Integer secondEventId = insertEvent(creatorId, "Evento B " + suffix, 4, false);

        EventResponse firstResponse = eventService.updateFeatured(firstEventId, admin);

        assertThat(firstResponse.id()).isEqualTo(firstEventId.longValue());
        assertThat(firstResponse.destaquePrincipal()).isTrue();
        assertThat(firstResponse.featured()).isTrue();
        assertThat(firstResponse.category()).isNotNull();
        assertThat(firstResponse.category().slug()).isNotBlank();
        assertThat(firstResponse.address()).isNotNull();
        assertThat(firstResponse.creator()).isNotNull();
        assertThat(firstResponse.creator().email()).startsWith("creator-");

        EventResponse secondResponse = eventService.updateFeatured(secondEventId, admin);

        assertThat(secondResponse.id()).isEqualTo(secondEventId.longValue());
        assertThat(secondResponse.destaquePrincipal()).isTrue();
        assertThat(manualFeaturedCount()).isEqualTo(1);
        assertThat(isManualFeatured(firstEventId)).isFalse();
        assertThat(isManualFeatured(secondEventId)).isTrue();

        List<EventResponse> featured = eventService.findFeatured();
        assertThat(featured).hasSize(1);
        assertThat(featured.get(0).id()).isEqualTo(secondEventId.longValue());
        assertThat(featured.get(0).category()).isNotNull();
        assertThat(featured.get(0).address()).isNotNull();
        assertThat(featured.get(0).creator()).isNotNull();

        eventService.clearFeatured(admin);

        assertThat(manualFeaturedCount()).isZero();
    }

    private Integer insertUser(String email, int roleId) {
        return jdbcTemplate.queryForObject("""
                insert into dbo.usuario (
                    nome, sobrenome, aniversario, numeroTelefone, email, senha, idCategoria,
                    pathImagemPerfil, emailVerificado, dataVerificacaoEmail
                )
                values ('Featured', 'Tester', '1990-01-01', '11999999999', ?, 'hash-for-test-only', ?,
                        '', 1, sysdatetime());
                select cast(scope_identity() as int)
                """, Integer.class, email, roleId);
    }

    private Integer insertEvent(Integer creatorId, String title, int startDayOffset, boolean featured) {
        Integer addressId = jdbcTemplate.queryForObject("""
                insert into dbo.endereco (cep, rua, cidade, estado, pais, numero)
                values ('00000000', 'Rua Featured', 'Sao Paulo', 'SP', 'Brasil', 1);
                select cast(scope_identity() as int)
                """, Integer.class);
        Integer contactId = jdbcTemplate.queryForObject("""
                insert into dbo.contatoEvento (email, numero)
                values (?, '11999999999');
                select cast(scope_identity() as int)
                """, Integer.class, "featured-contact-" + System.nanoTime() + "@luppa.test");
        return jdbcTemplate.queryForObject("""
                insert into dbo.evento (
                    idUsuario, idStatus, titulo, descricao, dataPostagem, pathImagemCapa,
                    dataInicio, dataFinal, idEndereco, idCategoria, idContato, destaquePrincipal
                )
                values (?, 0, ?, 'Descricao teste', sysdatetime(), '',
                        dateadd(day, ?, sysdatetime()), dateadd(day, ?, sysdatetime()), ?, 0, ?, ?);
                select cast(scope_identity() as int)
                """, Integer.class, creatorId, title, startDayOffset, startDayOffset + 1, addressId, contactId, featured);
    }

    private Integer manualFeaturedCount() {
        return jdbcTemplate.queryForObject("""
                select count(*) from dbo.evento where destaquePrincipal = 1
                """, Integer.class);
    }

    private boolean isManualFeatured(Integer eventId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select cast(destaquePrincipal as bit) from dbo.evento where idEvento = ?
                """, Boolean.class, eventId));
    }
}
