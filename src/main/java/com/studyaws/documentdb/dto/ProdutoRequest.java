package com.studyaws.documentdb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ProdutoRequest(
        @NotBlank String nome,
        @NotNull @Positive Double preco,
        @NotNull @PositiveOrZero Integer estoque
) {}