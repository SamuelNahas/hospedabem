package hospedabem.service;

import hospedabem.dao.HospedeDAO;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * UC06 Gerenciar Hóspede. As regras de formato ficam em {@link Hospede}; aqui ficam as que precisam
 * do banco: identificação não repetida e exclusão que não rompe vínculos com reservas ou
 * hospedagens. Toda gravação passa por {@link Hospede#validado()}, mesmo quando a tela já validou,
 * para nada chegar ao banco sem passar pelo domínio.
 */
@Service
public class ServicoHospede {
    private final HospedeDAO dao;
    private final Clock relogio;

    public ServicoHospede(HospedeDAO dao, Clock relogio) {
        this.dao = dao;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<Hospede> listar() {
        return dao.listar();
    }

    /** Busca por documento ou parte do nome, pela mesma grafia normalizada que a gravação usa. */
    @Transactional(readOnly = true)
    public List<Hospede> procurar(String termo) {
        return dao.procurar(termo);
    }

    @Transactional(readOnly = true)
    public Optional<Hospede> buscarPorDocumento(String cpfOuPassaporte) {
        return dao.buscarPorDocumento(cpfOuPassaporte);
    }

    @Transactional
    public Hospede incluir(Hospede informado) {
        Hospede h = validar(informado);
        return traduzindoDuplicidade(h, () -> h.comId(dao.inserir(h)));
    }

    /**
     * Grava {@code informado} sobre {@code original}, o cadastro como estava quando o formulário foi
     * aberto. Se outra estação alterou o hóspede nesse meio-tempo, recusa em vez de sobrescrever.
     */
    @Transactional
    public void alterar(Hospede original, Hospede informado) {
        if (original.id() != informado.id())
            throw new NegocioException("Os dados informados não são do hóspede selecionado. "
                    + "Selecione-o de novo na lista e refaça a alteração.");
        Hospede h = validar(informado);
        exigirInalterado(original, "refaça a alteração");
        traduzindoDuplicidade(h, () -> dao.atualizar(h));
    }

    /**
     * Exclui o hóspede se existir, estiver como o usuário confirmou e não houver reserva ou
     * hospedagem ligada a ele. A linha fica travada antes da contagem, então nenhuma estação cria
     * vínculo entre a contagem e o {@code DELETE}.
     */
    @Transactional
    public void excluir(Hospede h) {
        Hospede gravado = exigirInalterado(h, "confirme a exclusão de novo");
        int vinculos = dao.contarVinculos(gravado.id());
        if (vinculos > 0) throw comVinculos(gravado, vinculos);
        dao.excluir(gravado.id());
    }

    /** Trava a linha e recusa se outra estação a alterou depois que o usuário a selecionou. */
    private Hospede exigirInalterado(Hospede selecionado, String acao) {
        Hospede gravado = dao.buscarPorIdParaAtualizar(selecionado.id()).orElseThrow(ServicoHospede::inexistente);
        if (!gravado.equals(selecionado))
            throw new NegocioException("O hóspede " + gravado.nome() + " foi alterado por outra estação. "
                    + "Selecione-o de novo na lista e " + acao + ".");
        return gravado;
    }

    private Hospede validar(Hospede informado) {
        Hospede h = informado.validado();
        h.exigirNascimentoAte(LocalDate.now(relogio));
        return h;
    }

    /**
     * A identificação repetida é barrada pela restrição {@code UNIQUE} do banco, que vale também
     * entre estações simultâneas; aqui a violação é traduzida na mensagem que diz o que corrigir.
     */
    private <T> T traduzindoDuplicidade(Hospede h, java.util.function.Supplier<T> gravacao) {
        try {
            return gravacao.get();
        } catch (DuplicateKeyException e) {
            throw duplicado(h);
        }
    }

    private static NegocioException duplicado(Hospede h) {
        return new NegocioException("Já existe hóspede com a identificação " + h.cpfOuPassaporte()
                + ". Busque o cadastro existente e altere-o em vez de incluir outro.");
    }

    private static NegocioException comVinculos(Hospede h, int vinculos) {
        return new NegocioException("O hóspede " + h.nome() + " (" + h.cpfOuPassaporte() + ") não pode ser excluído: "
                + "há " + vinculos + " reserva(s)/hospedagem(ns) vinculada(s) a ele. O histórico é preservado; "
                + "altere os dados em vez de excluir.");
    }

    private static NegocioException inexistente() {
        return new NegocioException("Hóspede não encontrado; a lista pode estar desatualizada.");
    }
}
