package com.project.coopera_voto.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.coopera_voto.domain.enums.OpcaoVoto;
import com.project.coopera_voto.domain.model.entity.Pauta;
import com.project.coopera_voto.domain.model.entity.SessaoVotacao;
import com.project.coopera_voto.domain.records.ApiResponse;
import com.project.coopera_voto.service.pauta.PautaService;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pautas")
@RequiredArgsConstructor
public class PautaController {

    private final PautaService pautaService;

    @PostMapping
    public ResponseEntity<ApiResponse<Pauta>> criarPauta(@Valid @RequestBody Pauta pauta) {
        Pauta novaPauta = pautaService.criarPauta(pauta);
        ApiResponse<Pauta> response = new ApiResponse<>("success", "Pauta cadastrada com sucesso", novaPauta);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{pautaId}/sessao")
    public ResponseEntity<ApiResponse<SessaoVotacao>> abrirSessao(
            @PathVariable Long pautaId,
            @RequestParam(required = false) Long duracaoEmMinutos) {
        Duration duracao = (duracaoEmMinutos != null) ? Duration.ofMinutes(duracaoEmMinutos) : Duration.ofMinutes(1);
        SessaoVotacao sessao = pautaService.abrirSessao(pautaId, duracao);
        ApiResponse<SessaoVotacao> response = new ApiResponse<>("success", "Sessão aberta com sucesso", sessao);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pautaId}/votos")
    public ResponseEntity<ApiResponse<Void>> votar(
            @PathVariable Long pautaId,
            @RequestParam String cpf,
            @RequestParam OpcaoVoto opcao) {
        pautaService.votar(pautaId, cpf, opcao);
        ApiResponse<Void> response = new ApiResponse<>("success", "Voto computado com sucesso", null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{pautaId}/resultado")
    public ResponseEntity<ApiResponse<Map<OpcaoVoto, Long>>> resultadoVotacao(@PathVariable Long pautaId) {
        SessaoVotacao sessao = pautaService.buscarSessaoPorPauta(pautaId);
        Map<OpcaoVoto, Long> resultado = pautaService.contabilizarVotos(sessao.getId());
        ApiResponse<Map<OpcaoVoto, Long>> response = new ApiResponse<>("success", "Resultado da votação", resultado);
        return ResponseEntity.ok(response);
    }
}
