package com.project.coopera_voto.service.pauta;

import java.time.Duration;
import java.util.Map;

import com.project.coopera_voto.domain.enums.OpcaoVoto;
import com.project.coopera_voto.domain.model.entity.Pauta;
import com.project.coopera_voto.domain.model.entity.SessaoVotacao;

public interface PautaService {
    Pauta criarPauta(Pauta pauta);
    SessaoVotacao abrirSessao(Long pautaId, Duration duracao);
    void votar(Long pautaId, String cpf, OpcaoVoto opcao);
    Map<OpcaoVoto, Long> contabilizarVotos(Long sessaoId);
    SessaoVotacao buscarSessaoPorPauta(Long pautaId);
}
