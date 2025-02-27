package com.project.coopera_voto.service.pauta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.coopera_voto.application.exception.GenericOperationException;
import com.project.coopera_voto.domain.enums.OpcaoVoto;
import com.project.coopera_voto.domain.model.entity.Pauta;
import com.project.coopera_voto.domain.model.entity.SessaoVotacao;
import com.project.coopera_voto.domain.model.entity.Voto;
import com.project.coopera_voto.domain.repository.PautaRepository;
import com.project.coopera_voto.domain.repository.SessaoVotacaoRepository;
import com.project.coopera_voto.domain.repository.VotoRepository;
import com.project.coopera_voto.implementation.pauta.PautaServiceImpl;
import com.project.coopera_voto.service.kafka.KafkaProducerService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PautaServiceTest {

    @Mock
    private PautaRepository pautaRepository;
    
    @Mock
    private SessaoVotacaoRepository sessaoRepository;
    
    @Mock
    private VotoRepository votoRepository;
    
    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private PautaServiceImpl pautaService;

    @Test
    public void testCriarPauta_Success() {
        // Arrange
        Pauta pauta = new Pauta(null, "Título Teste", "Descrição Teste", LocalDateTime.now());
        Pauta savedPauta = new Pauta(1L, "Título Teste", "Descrição Teste", LocalDateTime.now());
        when(pautaRepository.save(any(Pauta.class))).thenReturn(savedPauta);

        // Act
        Pauta result = pautaService.criarPauta(pauta);

        // Assert
        assertNotNull(result.getId());
        assertEquals("Título Teste", result.getTitulo());
        verify(pautaRepository, times(1)).save(pauta);
    }

    @Test
    public void testAbrirSessao_Success() {
        // Arrange
        Pauta pauta = new Pauta(1L, "Título", "Descrição", LocalDateTime.now());
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.existsByPauta(pauta)).thenReturn(false);
        when(sessaoRepository.save(any(SessaoVotacao.class))).thenAnswer(invocation -> {
            SessaoVotacao s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        // Act
        SessaoVotacao sessao = pautaService.abrirSessao(1L, Duration.ofMinutes(2));

        // Assert
        assertNotNull(sessao.getId());
        assertEquals(pauta, sessao.getPauta());
        verify(sessaoRepository, times(1)).save(any(SessaoVotacao.class));
    }

    @Test
    public void testAbrirSessao_AlreadyOpen() {
        // Arrange
        Pauta pauta = new Pauta(1L, "Título", "Descrição", LocalDateTime.now());
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.existsByPauta(pauta)).thenReturn(true);

        // Act & Assert
        GenericOperationException ex = assertThrows(GenericOperationException.class,
                () -> pautaService.abrirSessao(1L, Duration.ofMinutes(1)));
        assertEquals("Sessão já aberta para essa pauta", ex.getMessage());
    }

    @Test
    public void testVotar_Success() {
        // Arrange
        Pauta pauta = new Pauta(1L, "Título", "Descrição", LocalDateTime.now());
        SessaoVotacao sessao = new SessaoVotacao(1L, pauta,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.existsBySessaoAndCpf(sessao, "12345678901")).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        pautaService.votar(1L, "12345678901", OpcaoVoto.SIM);

        // Assert
        verify(votoRepository, times(1)).save(any(Voto.class));
    }

    @Test
    public void testVotar_SessionClosed() {
        // Arrange
        Pauta pauta = new Pauta(1L, "Título", "Descrição", LocalDateTime.now());
        SessaoVotacao sessao = new SessaoVotacao(1L, pauta,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().minusMinutes(1));
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));

        // Act & Assert
        GenericOperationException ex = assertThrows(GenericOperationException.class,
                () -> pautaService.votar(1L, "12345678901", OpcaoVoto.SIM));
        assertEquals("Sessão de votação encerrada", ex.getMessage());
    }

    @Test
    public void testContabilizarVotos_Success() {
        // Arrange
        Pauta pauta = new Pauta(1L, "Título", "Descrição", LocalDateTime.now());
        SessaoVotacao sessao = new SessaoVotacao(1L, pauta,
                LocalDateTime.now().minusMinutes(2), LocalDateTime.now().minusMinutes(1));
        when(sessaoRepository.findById(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.countBySessaoAndOpcao(sessao, OpcaoVoto.SIM)).thenReturn(3L);
        when(votoRepository.countBySessaoAndOpcao(sessao, OpcaoVoto.NAO)).thenReturn(2L);

        // Act
        Map<OpcaoVoto, Long> resultado = pautaService.contabilizarVotos(1L);

        // Assert
        assertEquals(3L, resultado.get(OpcaoVoto.SIM));
        assertEquals(2L, resultado.get(OpcaoVoto.NAO));
        verify(kafkaProducerService, times(1)).sendResultadoVotacao(1L, resultado);
    }
}
