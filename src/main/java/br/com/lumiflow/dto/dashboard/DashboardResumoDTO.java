package br.com.lumiflow.dto.dashboard;

public record DashboardResumoDTO(

        Long totalOrdens,

        Long ordensAndamento,

        Long ordensConcluidas,

        Long ordensPendentes
) {}