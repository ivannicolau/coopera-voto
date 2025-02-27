package com.project.coopera_voto.implementation.pauta;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.project.coopera_voto.application.exception.GenericAlreadyExistsException;
import com.project.coopera_voto.application.exception.GenericNotFoundException;
import com.project.coopera_voto.application.exception.GenericOperationException;
import com.project.coopera_voto.domain.enums.OpcaoVoto;
import com.project.coopera_voto.domain.model.entity.Pauta;
import com.project.coopera_voto.domain.model.entity.SessaoVotacao;
import com.project.coopera_voto.domain.model.entity.Voto;
import com.project.coopera_voto.domain.repository.PautaRepository;
import com.project.coopera_voto.domain.repository.SessaoVotacaoRepository;
import com.project.coopera_voto.domain.repository.VotoRepository;
import com.project.coopera_voto.service.kafka.KafkaProducerService;
import com.project.coopera_voto.service.pauta.PautaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PautaServiceImpl implements PautaService {

    private final PautaRepository pautaRepo;
    private final SessaoVotacaoRepository sessaoRepo;
    private final VotoRepository votoRepo;
    private final KafkaProducerService kafkaProducer;

    @Override
    public Pauta criarPauta(Pauta pauta) {
        return pautaRepo.save(pauta);
    }

    @Override
    public SessaoVotacao abrirSessao(Long pautaId, Duration duracao) {
        Pauta pauta = pautaRepo.findById(pautaId)
            .orElseThrow(() -> new GenericNotFoundException("Pauta não encontrada"));

        if (sessaoRepo.existsByPauta(pauta)) {
            throw new GenericOperationException("Sessão já aberta para essa pauta");
        }
        
        SessaoVotacao sessao = new SessaoVotacao();
        sessao.setPauta(pauta);
        sessao.setDataInicio(LocalDateTime.now());
        sessao.setDataFim(LocalDateTime.now().plus(duracao != null ? duracao : Duration.ofMinutes(1)));
        sessao = sessaoRepo.save(sessao);
        
        agendarFechamentoSessao(sessao);
        return sessao;
    }
    
    private void agendarFechamentoSessao(SessaoVotacao sessao) {
        long delay = Duration.between(LocalDateTime.now(), sessao.getDataFim()).toMillis();
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            contabilizarVotos(sessao.getId());
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void votar(Long pautaId, String cpf, OpcaoVoto opcao) {
        SessaoVotacao sessao = sessaoRepo.findByPautaId(pautaId)
            .orElseThrow(() -> new GenericNotFoundException("Sessão não encontrada para a pauta"));
        
        if (!sessao.isAberta()) {
            throw new GenericOperationException("Sessão de votação encerrada");
        }
        
        if (votoRepo.existsBySessaoAndCpf(sessao, cpf)) {
            throw new GenericAlreadyExistsException("Associado já votou nesta pauta");
        }
        
        Voto voto = new Voto();
        voto.setSessao(sessao);
        voto.setCpf(cpf);
        voto.setOpcao(opcao);
        votoRepo.save(voto);
    }
    
    @Override
    public Map<OpcaoVoto, Long> contabilizarVotos(Long sessaoId) {
        SessaoVotacao sessao = sessaoRepo.findById(sessaoId)
            .orElseThrow(() -> new GenericNotFoundException("Sessão não encontrada"));
        
        Long votosSim = votoRepo.countBySessaoAndOpcao(sessao, OpcaoVoto.SIM);
        Long votosNao = votoRepo.countBySessaoAndOpcao(sessao, OpcaoVoto.NAO);
        
        Map<OpcaoVoto, Long> resultado = Map.of(
            OpcaoVoto.SIM, votosSim,
            OpcaoVoto.NAO, votosNao
        );
        
        kafkaProducer.sendResultadoVotacao(sessao.getPauta().getId(), resultado);
        return resultado;
    }
    
    @Override
    public SessaoVotacao buscarSessaoPorPauta(Long pautaId) {
        return sessaoRepo.findByPautaId(pautaId)
                .orElseThrow(() -> new GenericNotFoundException("Sessão não encontrada para a pauta"));
    }
}
