package com.project.coopera_voto.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.coopera_voto.domain.enums.OpcaoVoto;
import com.project.coopera_voto.domain.model.entity.SessaoVotacao;
import com.project.coopera_voto.domain.model.entity.Voto;

public interface VotoRepository extends JpaRepository<Voto, Long> {
    boolean existsBySessaoAndCpf(SessaoVotacao sessao, String cpf);
    Long countBySessaoAndOpcao(SessaoVotacao sessao, OpcaoVoto opcao);
}
