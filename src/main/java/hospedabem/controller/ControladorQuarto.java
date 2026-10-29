package hospedabem.controller;

import hospedabem.dao.QuartoDAO;
import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.NegocioException;
import hospedabem.model.Perfil;
import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UC04 Gerenciar Quarto (somente administrador). Regras RN-Q01 a RN-Q06 da expansão.
 * Qualquer recusa lança exceção dentro da transação: o rollback preserva os dados anteriores.
 */
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
        exigirSituacaoDeCadastro(q.getSituacao());
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
        Quarto atual = quartoDAO.buscarPorIdParaAtualizar(novo.getId())
                .orElseThrow(() -> new NegocioException("Quarto não encontrado."));
        tipoDAO.bloquear(List.of(atual.getTipo().id(), novo.getTipo().id()));
        boolean mudouTipo = atual.getTipo().id() != novo.getTipo().id();
        if (quartoDAO.possuiHospedagem(atual.getId(), true)) {
            if (mudouTipo || novo.getSituacao() != SituacaoQuarto.OCUPADO)
                throw new NegocioException("Quarto " + atual.getNumero() + " tem hospedagem ativa: não pode ser liberado, "
                        + "reclassificado ou colocado em manutenção pelo cadastro. A liberação ocorre no check-out.");
        } else {
            exigirSituacaoDeCadastro(novo.getSituacao());
        }
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
        Quarto atual = quartoDAO.buscarPorIdParaAtualizar(q.getId())
                .orElseThrow(() -> new NegocioException("Quarto não encontrado."));
        tipoDAO.bloquear(List.of(atual.getTipo().id()));
        if (quartoDAO.possuiHospedagem(atual.getId(), false))
            throw new NegocioException("Quarto " + atual.getNumero() + " está vinculado a hospedagens e não pode ser excluído.");
        quartoDAO.excluir(atual.getId());
        disponibilidade.garantirCompromissos(atual.getTipo());
    }

    /** RN-Q02: ocupado e reservado decorrem de check-in/reserva; o cadastro só define disponível ou manutenção. */
    private static void exigirSituacaoDeCadastro(SituacaoQuarto s) {
        if (s != SituacaoQuarto.DISPONIVEL && s != SituacaoQuarto.MANUTENCAO)
            throw new NegocioException("Situação " + s + " não pode ser definida pelo cadastro: ela resulta do check-in "
                    + "ou da reserva. Use DISPONIVEL ou MANUTENCAO.");
    }

    private void exigirTipoExistente(Quarto q) {
        if (tipoDAO.buscarPorCodigo(q.getTipo().codigo()).filter(t -> t.id() == q.getTipo().id()).isEmpty())
            throw new NegocioException("Tipo de quarto não cadastrado: " + q.getTipo().codigo());
    }

    /** A unicidade do número é garantida pela restrição UNIQUE; aqui só se traduz a mensagem. */
    private static NegocioException numeroDuplicado(Quarto q) {
        return new NegocioException("Já existe quarto com o número " + q.getNumero() + ".");
    }
}
