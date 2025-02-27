package com.project.coopera_voto.domain.records;

import jakarta.validation.constraints.NotBlank;

public record PautaRequestDTO(
    @NotBlank(message = "O título é obrigatório")
    String titulo,
    String descricao
) {}
