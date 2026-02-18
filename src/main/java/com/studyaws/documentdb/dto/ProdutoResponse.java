package com.studyaws.documentdb.dto;

public record ProdutoResponse(
        String id,
        String nome,
        Double preco,
        Integer estoque
) {}