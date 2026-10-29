package hospedabem.controller;

import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import hospedabem.model.TipoQuarto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ControladorTipoQuarto {
    private final Sessao sessao;
    private final TipoQuartoDAO dao;
    private final Clock relogio;

    public ControladorTipoQuarto(Sessao sessao, TipoQuartoDAO dao, Clock relogio) {
        this.sessao = sessao;
        this.dao = dao;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<TipoQuarto> listar() {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return dao.listar();
    }

    @Transactional(readOnly = true)
    public Optional<TipoQuarto> buscar(String codigo) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return dao.buscarPorCodigo(codigo);
    }

    @Transactional
    public TipoQuarto incluir(TipoQuarto t) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        try {
            return new TipoQuarto(dao.inserir(t), t.codigo(), t.descricao(), t.capacidadeMaxima(), t.valorDiaria());
        } catch (DuplicateKeyException e) {
            throw codigoDuplicado(t);
        }
    }

    @Transactional
    public void alterar(TipoQuarto t) {
        sessao.exigir(Perfil.ADMINISTRADOR);
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
        sessao.exigir(Perfil.ADMINISTRADOR);
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
