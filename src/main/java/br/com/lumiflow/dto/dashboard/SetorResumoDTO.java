package br.com.lumiflow.dto.dashboard;

public record SetorResumoDTO(
        Long id,
        String nome,
        long qtdOrdens
) {}