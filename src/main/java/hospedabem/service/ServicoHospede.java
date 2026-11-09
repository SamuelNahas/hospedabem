package hospedabem.service;

import hospedabem.dao.HospedeDAO;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional
    public void alterar(Hospede informado) {
        Hospede h = validar(informado);
        if (traduzindoDuplicidade(h, () -> dao.atualizar(h)) == 0) throw inexistente();
    }

    /**
     * Exclui o hóspede se existir e não houver reserva ou hospedagem ligada a ele. O impedimento é
     * conferido na mesma transação e, ainda assim, a violação de chave estrangeira é traduzida: a
     * reserva pode ser registrada por outra estação entre a contagem e o {@code DELETE}.
     */
    @Transactional
    public void excluir(Hospede h) {
        Hospede gravado = dao.buscarPorId(h.id()).orElseThrow(ServicoHospede::inexistente);
        int vinculos = dao.contarVinculos(gravado.id());
        if (vinculos > 0) throw comVinculos(gravado, vinculos);
        try {
            if (dao.excluir(gravado.id()) == 0) throw inexistente();
        } catch (DataIntegrityViolationException e) {
            throw comVinculos(gravado, dao.contarVinculos(gravado.id()));
        }
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
