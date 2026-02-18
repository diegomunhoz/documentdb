package com.studyaws.documentdb.service;

import com.studyaws.documentdb.dto.ProdutoRequest;
import com.studyaws.documentdb.dto.ProdutoResponse;
import com.studyaws.documentdb.exception.ResourceNotFoundException;
import com.studyaws.documentdb.model.Produto;
import com.studyaws.documentdb.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public List<ProdutoResponse> listar() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProdutoResponse buscar(String id) {
        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        return toResponse(produto);
    }

    public ProdutoResponse salvar(ProdutoRequest request) {
        Produto produto = new Produto();
        produto.setNome(request.nome());
        produto.setPreco(request.preco());
        produto.setEstoque(request.estoque());
        return toResponse(repository.save(produto));
    }

    public ProdutoResponse atualizar(String id, ProdutoRequest request) {
        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        produto.setNome(request.nome());
        produto.setPreco(request.preco());
        produto.setEstoque(request.estoque());
        return toResponse(repository.save(produto));
    }

    public void deletar(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Produto não encontrado");
        }
        repository.deleteById(id);
    }

    private ProdutoResponse toResponse(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getPreco(),
                produto.getEstoque()
        );
    }
}