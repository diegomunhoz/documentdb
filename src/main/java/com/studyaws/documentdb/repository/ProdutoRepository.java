package com.studyaws.documentdb.repository;

import com.studyaws.documentdb.model.Produto;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProdutoRepository extends MongoRepository<Produto, String> {
}