package com.project.coopera_voto.service.kafka;

import java.util.Map;

import com.project.coopera_voto.domain.enums.OpcaoVoto;

public interface KafkaProducerService {
    void sendResultadoVotacao(Long pautaId, Map<OpcaoVoto, Long> resultado);
}
