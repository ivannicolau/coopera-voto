package com.project.coopera_voto.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.coopera_voto.domain.model.entity.Pauta;
import com.project.coopera_voto.domain.model.entity.SessaoVotacao;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {
    Optional<SessaoVotacao> findByPautaId(Long pautaId);
    boolean existsByPauta(Pauta pauta);
}
