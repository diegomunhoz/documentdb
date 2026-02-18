package com.studyaws.documentdb.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("produtos")
@CompoundIndex(def = "{'nome':1}", unique = true)
public class Produto {
    @Id
    private String id;
    private String nome;
    private Double preco;
    private Integer estoque;
}