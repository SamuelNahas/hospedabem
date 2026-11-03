package hospedabem.service;

import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.NegocioException;
import hospedabem.model.TipoQuarto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ServicoTipoQuarto {
    private final TipoQuartoDAO dao;
    private final Clock relogio;

    public ServicoTipoQuarto(TipoQuartoDAO dao, Clock relogio) {
        this.dao = dao;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<TipoQuarto> listar() {
        return dao.listar();
    }

    @Transactional(readOnly = true)
    public Optional<TipoQuarto> buscar(String codigo) {
        return dao.buscarPorCodigo(codigo);
    }

    @Transactional
    public TipoQuarto incluir(TipoQuarto t) {
        try {
            return new TipoQuarto(dao.inserir(t), t.codigo(), t.descricao(), t.capacidadeMaxima(), t.valorDiaria());
        } catch (DuplicateKeyException e) {
            throw codigoDuplicado(t);
        }
    }

    @Transactional
    public void alterar(TipoQuarto t) {
        dao.bloquear(List.of(t.id()));
        t.exigirComporta(dao.maiorOcupacaoVigente(t.id(), LocalDate.now(relogio)));
        try {
            dao.atualizar(t);
        } catch (DuplicateKeyException e) {
            throw codigoDuplicado(t);
        }
    }

    @Transactional
    public void excluir(TipoQuarto t) {
        try {
            dao.excluir(t.id());
        } catch (DataIntegrityViolationException e) {
            throw new NegocioException("Tipo " + t.codigo() + " não pode ser excluído: há quartos ou reservas vinculados.");
        }
    }

    private static NegocioException codigoDuplicado(TipoQuarto t) {
        return new NegocioException("Já existe tipo de quarto com o código " + t.codigo() + ".");
    }
}
