package br.com.techchallenge.mecanica.auth.application.gateway;

import java.util.Optional;

import br.com.techchallenge.mecanica.auth.domain.Client;
import br.com.techchallenge.mecanica.auth.domain.CpfCnpj;

@FunctionalInterface
public interface ClientRepository {

    Optional<Client> findByCpfCnpj(CpfCnpj cpfCnpj);
}