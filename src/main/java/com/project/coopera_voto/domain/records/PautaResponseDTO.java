package com.project.coopera_voto.domain.records;

import java.time.LocalDateTime;

public record PautaResponseDTO(
    Long id,
    String titulo,
    String descricao,
    LocalDateTime dataCriacao
) {}
