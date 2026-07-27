package br.com.techchallenge.mecanica.auth.infrastructure.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import br.com.techchallenge.mecanica.auth.application.gateway.ClientRepository;
import br.com.techchallenge.mecanica.auth.domain.Client;
import br.com.techchallenge.mecanica.auth.domain.CpfCnpj;

public class JdbcClientRepository implements ClientRepository {

    private static final String FIND_BY_DOCUMENT_SQL = """
            SELECT id, ativo
            FROM cliente_jpa_entity
            WHERE cpf_cnpj = ?
            LIMIT 1
            """;

    private final DataSource dataSource;

    public JdbcClientRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<Client> findByCpfCnpj(CpfCnpj cpfCnpj) {
        Objects.requireNonNull(cpfCnpj);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(FIND_BY_DOCUMENT_SQL)) {

            statement.setString(1, cpfCnpj.value());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                UUID clientId = resultSet.getObject("id", UUID.class);
                boolean active = resultSet.getBoolean("ativo");

                return Optional.of(new Client(clientId, active));
            }
        } catch (SQLException exception) {
            throw new ClientRepositoryException(
                    "Falha ao consultar o cliente",
                    exception);
        }
    }
}