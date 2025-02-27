package com.project.coopera_voto.application.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.coopera_voto.domain.enums.OpcaoVoto;
import com.project.coopera_voto.service.kafka.KafkaProducerService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    // Substituímos o bean real do KafkaProducerService por um mock para podermos verificar as interações
    @MockBean
    private KafkaProducerService kafkaProducerService;

    // Método utilitário para criar uma nova pauta via API e extrair o ID da resposta
    private Long createPauta(String titulo, String descricao) throws Exception {
        String pautaJson = String.format("{\"titulo\": \"%s\", \"descricao\": \"%s\"}", titulo, descricao);
        String response = mockMvc.perform(post("/api/v1/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pautaJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("data").get("id").asLong();
    }

    // Método utilitário para abrir uma sessão para uma pauta e extrair o ID da sessão
    private Long abrirSessao(Long pautaId, Long duracaoEmMinutos) throws Exception {
        String url = String.format("/api/v1/pautas/%d/sessao?duracaoEmMinutos=%d", pautaId, duracaoEmMinutos);
        String response = mockMvc.perform(post(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("data").get("id").asLong();
    }

    @Test
    public void testCriarPauta_Success() throws Exception {
        String pautaJson = "{\"titulo\": \"Pauta de Teste\", \"descricao\": \"Descrição de teste\"}";

        mockMvc.perform(post("/api/v1/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pautaJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.titulo").value("Pauta de Teste"));
    }

    @Test
    public void testCriarPauta_ValidationError() throws Exception {
        // Título vazio dispara validação
        String pautaJson = "{\"titulo\": \"\", \"descricao\": \"Descrição de teste\"}";

        mockMvc.perform(post("/api/v1/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pautaJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Erro de validação"));
    }

    @Test
    public void testAbrirSessao_Success() throws Exception {
        Long pautaId = createPauta("Pauta Sessão", "Descrição sessão");
        String url = String.format("/api/v1/pautas/%d/sessao?duracaoEmMinutos=2", pautaId);

        mockMvc.perform(post(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    public void testAbrirSessao_PautaNotFound() throws Exception {
        // Tenta abrir sessão para uma pauta inexistente (id 99999)
        String url = "/api/v1/pautas/99999/sessao?duracaoEmMinutos=2";

        mockMvc.perform(post(url))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Pauta não encontrada"));
    }

    @Test
    public void testVotar_SuccessAndDuplicateVote() throws Exception {
        // Cria pauta e abre sessão
        Long pautaId = createPauta("Pauta Votação", "Descrição votação");
        abrirSessao(pautaId, 2L);

        String cpf = "12345678901";
        String urlVotar = String.format("/api/v1/pautas/%d/votos?cpf=%s&opcao=SIM", pautaId, cpf);

        // Primeiro voto deve ser bem-sucedido
        mockMvc.perform(post(urlVotar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Voto computado com sucesso"));

        // Segundo voto com o mesmo CPF deve retornar erro de conflito
        mockMvc.perform(post(urlVotar))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Associado já votou nesta pauta"));
    }

    @Test
    public void testResultadoVotacao_Success() throws Exception {
        // Fluxo completo: criar pauta, abrir sessão, realizar votos e obter resultado
        Long pautaId = createPauta("Pauta Resultado", "Descrição resultado");
        abrirSessao(pautaId, 2L);

        String urlVotar = String.format("/api/v1/pautas/%d/votos", pautaId);

        // Voto SIM com diferentes CPFs
        mockMvc.perform(post(urlVotar)
                .param("cpf", "11111111111")
                .param("opcao", "SIM"))
                .andExpect(status().isOk());

        // Voto NAO
        mockMvc.perform(post(urlVotar)
                .param("cpf", "22222222222")
                .param("opcao", "NAO"))
                .andExpect(status().isOk());

        // Outro voto SIM
        mockMvc.perform(post(urlVotar)
                .param("cpf", "33333333333")
                .param("opcao", "SIM"))
                .andExpect(status().isOk());

        // Consulta do resultado da votação
        String urlResultado = String.format("/api/v1/pautas/%d/resultado", pautaId);
        String response = mockMvc.perform(get(urlResultado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        JsonNode resultado = node.get("data");

        // Verifica os contadores de votos
        assertEquals(2, resultado.get("SIM").asInt());
        assertEquals(1, resultado.get("NAO").asInt());

        // Verifica que o KafkaProducerService foi chamado para enviar o resultado
        verify(kafkaProducerService, times(1)).sendResultadoVotacao(pautaId,
                Map.of(OpcaoVoto.SIM, 2L, OpcaoVoto.NAO, 1L));
    }
}
