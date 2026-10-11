package com.luppa.api.controller;

import com.luppa.api.dto.RatingResponse;
import com.luppa.api.support.RequiresIsolatedSqlServerTestDatabase;
import com.luppa.api.service.AccountEmailService;
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
class RatingControllerIntegrationTest {
    @Autowired
    private RatingController ratingController;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AccountEmailService accountEmailService;

    @Test
    void publicEventRatingsEndpointReturnsMultipleExistingRatingsWithOnlyPublicAuthorData() {
        String suffix = Long.toString(System.nanoTime());
        Integer eventId = createEventWithRatings(suffix);

        List<RatingResponse> responses = ratingController.byEvent(eventId);

        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(RatingResponse::description)
                .containsExactly("Review tres", "Review dois", "Review um");
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.eventId()).isEqualTo(eventId.longValue());
            assertThat(response.user().name()).startsWith("Public Reviewer");
            assertThat(response.user().profileImageUrl()).doesNotContain("null");
            assertThat(response.user().getClass().getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .containsExactly("id", "name", "profileImageUrl");
        });
    }

    private Integer createEventWithRatings(String suffix) {
        Integer creatorId = insertUser("creator-" + suffix + "@luppa.test", "Creator", "Test", null);
        Integer addressId = jdbcTemplate.queryForObject("""
                insert into dbo.endereco (cep, rua, cidade, estado, pais, numero)
                values ('00000000', 'Rua Teste', 'Sao Paulo', 'SP', 'Brasil', 1);
                select cast(scope_identity() as int)
                """, Integer.class);
        Integer contactId = jdbcTemplate.queryForObject("""
                insert into dbo.contatoEvento (email, numero)
                values (?, '11999999999');
                select cast(scope_identity() as int)
                """, Integer.class, "contact-" + suffix + "@luppa.test");
        Integer eventId = jdbcTemplate.queryForObject("""
                insert into dbo.evento (
                    idUsuario, idStatus, titulo, descricao, dataPostagem, pathImagemCapa,
                    dataInicio, dataFinal, idEndereco, idCategoria, idContato
                )
                values (?, 0, 'Evento Teste', 'Descricao teste', sysdatetime(), '',
                        dateadd(day, -2, sysdatetime()), dateadd(day, -1, sysdatetime()), ?, 0, ?);
                select cast(scope_identity() as int)
                """, Integer.class, creatorId, addressId, contactId);

        insertRating(eventId, insertUser("reviewer-1-" + suffix + "@luppa.test", "Public", "Reviewer Um", "reviewer1.png"), 5, "Review um", -3);
        insertRating(eventId, insertUser("reviewer-2-" + suffix + "@luppa.test", "Public", "Reviewer Dois", ""), 4, "Review dois", -2);
        insertRating(eventId, insertUser("reviewer-3-" + suffix + "@luppa.test", "Public", "Reviewer Tres", null), 3, "Review tres", -1);
        return eventId;
    }

    private Integer insertUser(String email, String firstName, String lastName, String profileImagePath) {
        return jdbcTemplate.queryForObject("""
                insert into dbo.usuario (
                    nome, sobrenome, aniversario, numeroTelefone, email, senha, idCategoria,
                    pathImagemPerfil, emailVerificado, dataVerificacaoEmail
                )
                values (?, ?, '1990-01-01', '11999999999', ?, ?, 2, ?, 1, sysdatetime());
                select cast(scope_identity() as int)
                """, Integer.class, firstName, lastName, email, "hash-for-test-only", profileImagePath);
    }

    private void insertRating(Integer eventId, Integer userId, int score, String description, int minutesOffset) {
        jdbcTemplate.update("""
                insert into dbo.avaliacao (dataAvaliacao, nota, descricao, idEvento, idUsuario)
                values (dateadd(minute, ?, sysdatetime()), ?, ?, ?, ?)
                """, minutesOffset, score, description, eventId, userId);
    }
}
