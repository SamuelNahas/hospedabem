package hospedabem.controller;

import hospedabem.dao.QuartoDAO;
import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import hospedabem.model.Quarto;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ControladorQuarto {
    private final Sessao sessao;
    private final QuartoDAO quartoDAO;
    private final TipoQuartoDAO tipoDAO;
    private final ServicoDisponibilidade disponibilidade;

    public ControladorQuarto(Sessao sessao, QuartoDAO quartoDAO, TipoQuartoDAO tipoDAO,
                             ServicoDisponibilidade disponibilidade) {
        this.sessao = sessao;
        this.quartoDAO = quartoDAO;
        this.tipoDAO = tipoDAO;
        this.disponibilidade = disponibilidade;
    }

    @Transactional(readOnly = true)
    public List<Quarto> listar() {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return quartoDAO.listar();
    }

    @Transactional
    public Quarto incluir(Quarto q) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        q.exigirSituacaoDeCadastro();
        exigirTipoExistente(q);
        tipoDAO.bloquear(List.of(q.getTipo().id()));
        try {
            return new Quarto(quartoDAO.inserir(q), q.getNumero(), q.getAndar(), q.getTipo(), q.getSituacao());
        } catch (DuplicateKeyException e) {
            throw numeroDuplicado(q);
        }
    }

    @Transactional
    public void alterar(Quarto novo) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        exigirTipoExistente(novo);
        Quarto atual = buscarParaAtualizar(novo.getId());
        tipoDAO.bloquear(List.of(atual.getTipo().id(), novo.getTipo().id()));
        atual.exigirAlteracaoPara(novo);
        try {
            quartoDAO.atualizar(novo);
        } catch (DuplicateKeyException e) {
            throw numeroDuplicado(novo);
        }
        disponibilidade.garantirCompromissos(atual.getTipo());
    }

    @Transactional
    public void excluir(Quarto q) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        Quarto atual = buscarParaAtualizar(q.getId());
        tipoDAO.bloquear(List.of(atual.getTipo().id()));
        if (quartoDAO.possuiHospedagens(atual.getId()))
            throw new NegocioException("Quarto " + atual.getNumero() + " está vinculado a hospedagens e não pode ser excluído.");
        quartoDAO.excluir(atual.getId());
        disponibilidade.garantirCompromissos(atual.getTipo());
    }

    private Quarto buscarParaAtualizar(int id) {
        return quartoDAO.buscarPorIdParaAtualizar(id).orElseThrow(() -> new NegocioException("Quarto não encontrado."));
    }

    private void exigirTipoExistente(Quarto q) {
        if (tipoDAO.buscarPorCodigo(q.getTipo().codigo()).filter(t -> t.id() == q.getTipo().id()).isEmpty())
            throw new NegocioException("Tipo de quarto não cadastrado: " + q.getTipo().codigo());
    }

    private static NegocioException numeroDuplicado(Quarto q) {
        return new NegocioException("Já existe quarto com o número " + q.getNumero() + ".");
    }
}
