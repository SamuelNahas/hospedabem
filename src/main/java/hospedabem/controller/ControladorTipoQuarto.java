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

/** UC03 Gerenciar Tipo de Quarto (somente administrador, RF-3). */
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
        exigirCodigoLivre(t);
        try {
            int id = dao.inserir(t);
            return new TipoQuarto(id, t.codigo(), t.descricao(), t.capacidadeMaxima(), t.valorDiaria());
        } catch (DuplicateKeyException e) {
            throw codigoDuplicado(t);
        }
    }

    /** Capacidade não pode ficar abaixo do exigido por reservas vigentes ou hospedagens ativas. */
    @Transactional
    public void alterar(TipoQuarto t) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        dao.bloquear(List.of(t.id()));
        exigirCodigoLivre(t);
        int exigido = dao.maiorOcupacaoVigente(t.id(), LocalDate.now(relogio));
        if (t.capacidadeMaxima() < exigido)
            throw new NegocioException("Capacidade não pode ser reduzida para " + t.capacidadeMaxima()
                    + ": há reserva ou hospedagem deste tipo com " + exigido + " hóspede(s).");
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

    private void exigirCodigoLivre(TipoQuarto t) {
        dao.buscarPorCodigo(t.codigo()).filter(existente -> existente.id() != t.id()).ifPresent(e -> {
            throw codigoDuplicado(t);
        });
    }

    private static NegocioException codigoDuplicado(TipoQuarto t) {
        return new NegocioException("Já existe tipo de quarto com o código " + t.codigo() + ".");
    }
}
