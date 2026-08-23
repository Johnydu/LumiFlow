package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.entity.enums.TipoVidro;

public record SaldoVidroDTO(

        Long chapaVidroId,

        TipoVidro tipoVidro,

        String descricao,

        Integer estoqueAtual,

        Integer estoqueMinimo,

        boolean abaixoDoEstoqueMinimo
) {}
