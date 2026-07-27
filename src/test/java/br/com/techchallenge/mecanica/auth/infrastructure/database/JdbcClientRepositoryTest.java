package br.com.techchallenge.mecanica.auth.infrastructure.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.techchallenge.mecanica.auth.domain.CpfCnpj;

class JdbcClientRepositoryTest {

    private static final UUID ACTIVE_CLIENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID INACTIVE_CLIENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000002");

    private JdbcDataSource dataSource;
    private JdbcClientRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setURL(
                "jdbc:h2:mem:auth-"
                        + UUID.randomUUID()
                        + ";MODE=PostgreSQL"
                        + ";DATABASE_TO_LOWER=TRUE"
                        + ";DB_CLOSE_DELAY=-1");

        createSchema();
        repository = new JdbcClientRepository(dataSource);
    }

    @Test
    void shouldFindActiveClientByFormattedCpf() throws SQLException {
        insertClient(ACTIVE_CLIENT_ID, "52998224725", true);

        var result = repository.findByCpfCnpj(
                new CpfCnpj("529.982.247-25"));

        assertTrue(result.isPresent());
        assertEquals(ACTIVE_CLIENT_ID, result.orElseThrow().id());
        assertTrue(result.orElseThrow().active());
    }

    @Test
    void shouldFindInactiveClientWithoutHidingItsStatus()
            throws SQLException {

        insertClient(INACTIVE_CLIENT_ID, "11222333000181", false);

        var result = repository.findByCpfCnpj(
                new CpfCnpj("11.222.333/0001-81"));

        assertTrue(result.isPresent());
        assertEquals(INACTIVE_CLIENT_ID, result.orElseThrow().id());
        assertFalse(result.orElseThrow().active());
    }

    @Test
    void shouldReturnEmptyWhenClientDoesNotExist() {
        var result = repository.findByCpfCnpj(
                new CpfCnpj("52998224725"));

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldTranslateDatabaseFailure() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            statement.execute("DROP TABLE cliente_jpa_entity");
        }

        assertThrows(
                ClientRepositoryException.class,
                () -> repository.findByCpfCnpj(
                        new CpfCnpj("52998224725")));
    }

    @Test
    void shouldRequireDataSource() {
        assertThrows(
                NullPointerException.class,
                () -> new JdbcClientRepository(null));
    }

    @Test
    void shouldRequireDocument() {
        assertThrows(
                NullPointerException.class,
                () -> repository.findByCpfCnpj(null));
    }

    private void createSchema() throws SQLException {
        String sql = """
                CREATE TABLE cliente_jpa_entity (
                    id UUID PRIMARY KEY,
                    cpf_cnpj VARCHAR(14) NOT NULL UNIQUE,
                    ativo BOOLEAN NOT NULL
                )
                """;

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            statement.execute(sql);
        }
    }

    private void insertClient(
            UUID id,
            String cpfCnpj,
            boolean active) throws SQLException {

        String sql = """
                INSERT INTO cliente_jpa_entity (id, cpf_cnpj, ativo)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setObject(1, id);
            statement.setString(2, cpfCnpj);
            statement.setBoolean(3, active);
            statement.executeUpdate();
        }
    }
}