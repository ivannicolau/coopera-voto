package com.project.coopera_voto.implementation.kafka;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.project.coopera_voto.domain.enums.OpcaoVoto;
import com.project.coopera_voto.service.kafka.KafkaProducerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void sendResultadoVotacao(Long pautaId, Map<OpcaoVoto, Long> resultado) {
        Map<String, Object> mensagem = new HashMap<>();
        mensagem.put("pautaId", pautaId);
        mensagem.put("resultado", resultado);
        mensagem.put("timestamp", LocalDateTime.now());
        kafkaTemplate.send("voting_result", mensagem);
    }
}
