package com.project.coopera_voto.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.coopera_voto.domain.model.entity.Pauta;

public interface PautaRepository extends JpaRepository<Pauta, Long> { }
