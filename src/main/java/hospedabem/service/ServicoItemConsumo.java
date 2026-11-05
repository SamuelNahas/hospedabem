package hospedabem.service;

import hospedabem.dao.ItemConsumoDAO;
import hospedabem.model.ItemConsumo;
import hospedabem.model.NegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ServicoItemConsumo {
    private final ItemConsumoDAO dao;

    public ServicoItemConsumo(ItemConsumoDAO dao) {
        this.dao = dao;
    }

    /** Retorna o catálogo persistido em ordem de código, disponível para integração com o UC11. */
    @Transactional(readOnly = true)
    public List<ItemConsumo> listar() {
        return dao.listar();
    }

    @Transactional(readOnly = true)
    public Optional<ItemConsumo> buscar(String codigo) {
        return dao.buscarPorCodigo(codigo);
    }

    /** Inclui o item e traduz conflitos de código em mensagem de negócio, sem gravação parcial. */
    @Transactional
    public ItemConsumo incluir(ItemConsumo item) {
        try {
            return new ItemConsumo(dao.inserir(item), item.codigo(), item.descricao(), item.categoria(), item.preco());
        } catch (DuplicateKeyException e) {
            throw codigoDuplicado(item);
        }
    }

    /** Altera somente o catálogo; recusa item inexistente ou código já utilizado. */
    @Transactional
    public void alterar(ItemConsumo item) {
        try {
            if (dao.atualizar(item) == 0) throw itemInexistente();
        } catch (DuplicateKeyException e) {
            throw codigoDuplicado(item);
        }
    }

    /** Exclui o item se existir e não houver consumos vinculados, preservando o histórico. */
    @Transactional
    public void excluir(ItemConsumo item) {
        try {
            if (dao.excluir(item.id()) == 0) throw itemInexistente();
        } catch (DataIntegrityViolationException e) {
            throw new NegocioException("Item " + item.codigo() + " não pode ser excluído: há consumos vinculados.");
        }
    }

    private static NegocioException codigoDuplicado(ItemConsumo item) {
        return new NegocioException("Já existe item com o código " + item.codigo() + ".");
    }

    private static NegocioException itemInexistente() {
        return new NegocioException("Item de consumo ou serviço não encontrado.");
    }
}
