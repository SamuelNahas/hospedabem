package hospedabem.service;

import hospedabem.dao.HospedeDAO;
import hospedabem.dao.ReservaDAO;
import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import hospedabem.model.Periodo;
import hospedabem.model.Reserva;
import hospedabem.model.SolicitacaoReserva;
import hospedabem.model.TipoQuarto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * UC08 Registrar Reserva e UC09 Consultar, Alterar e Cancelar Reserva. As regras ficam em
 * {@link SolicitacaoReserva} e em {@link Reserva}; aqui ficam a transação, os bloqueios e o
 * recálculo de disponibilidade, que precisam do banco.
 */
@Service
public class ServicoReserva {
    private final HospedeDAO hospedeDAO;
    private final TipoQuartoDAO tipoDAO;
    private final ReservaDAO reservaDAO;
    private final ServicoDisponibilidade disponibilidade;
    private final Clock relogio;

    public ServicoReserva(HospedeDAO hospedeDAO, TipoQuartoDAO tipoDAO, ReservaDAO reservaDAO,
                          ServicoDisponibilidade disponibilidade, Clock relogio) {
        this.hospedeDAO = hospedeDAO;
        this.tipoDAO = tipoDAO;
        this.reservaDAO = reservaDAO;
        this.disponibilidade = disponibilidade;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Hospede buscarResponsavel(String cpfOuPassaporte) {
        return hospedeDAO.buscarPorDocumento(cpfOuPassaporte).orElseThrow(() -> new NegocioException(
                "Hóspede não cadastrado: " + cpfOuPassaporte + ". Cadastre-o em \"Hóspedes\" (UC06) e registre a "
                        + "reserva em seguida."));
    }

    @Transactional
    public Reserva registrar(int funcionarioId, String cpfResponsavel, String codigoTipo, Periodo periodo, int quantidade) {
        TipoQuarto tipo = buscarTipo(codigoTipo);
        Hospede responsavel = buscarResponsavel(cpfResponsavel);
        if (hospedeDAO.bloquear(List.of(responsavel.id())) == 0)
            throw new NegocioException("O hóspede " + responsavel.nome() + " foi excluído por outra estação.");
        var solicitacao = new SolicitacaoReserva(responsavel, tipo, periodo, quantidade);
        solicitacao.validar(LocalDate.now(relogio));
        tipoDAO.bloquear(List.of(tipo.id()));
        disponibilidade.calcular(tipo, solicitacao.periodo()).exigirVaga();
        return reservaDAO.buscarPorCodigo(reservaDAO.inserir(solicitacao, funcionarioId)).orElseThrow();
    }

    /** UC09: localiza a reserva pelo código para a tela mostrar os dados e a situação antes de agir. */
    @Transactional(readOnly = true)
    public Reserva consultar(int codigo) {
        return reservaDAO.buscarPorCodigo(codigo).orElseThrow(() -> inexistente(codigo));
    }

    /**
     * UC09: grava os dados informados sobre {@code selecionada}, a reserva como estava quando a tela
     * a localizou. Os bloqueios seguem a mesma ordem do check-in (UC10) — tipos de quarto primeiro,
     * em ordem crescente de id, depois a reserva, depois os hóspedes — para que as duas operações
     * nunca se esperem em sentidos opostos. Quando o tipo muda, os dois tipos envolvidos entram no
     * mesmo {@code bloquear}, que ordena os ids: a origem devolve capacidade e o destino a consome,
     * e nenhuma outra transação vê um dos dois livre enquanto esta não termina.
     */
    @Transactional
    public Reserva alterar(Reserva selecionada, String cpfResponsavel, String codigoTipo, Periodo periodo,
                           int quantidade) {
        TipoQuarto destino = buscarTipo(codigoTipo);
        Hospede responsavel = buscarResponsavel(cpfResponsavel);
        tipoDAO.bloquear(List.of(selecionada.getTipo().id(), destino.id()));
        Reserva gravada = exigirInalterada(selecionada, "refaça a alteração");
        if (hospedeDAO.bloquear(List.of(responsavel.id())) == 0)
            throw new NegocioException("O hóspede " + responsavel.nome() + " foi excluído por outra estação.");
        Reserva alterada = gravada.alteradaPara(new SolicitacaoReserva(responsavel, destino, periodo, quantidade),
                LocalDate.now(relogio));
        // A própria reserva sai do recálculo: a vaga que ela já ocupa continua sendo dela.
        disponibilidade.calcular(destino, alterada.getPeriodo(), gravada).exigirVaga();
        reservaDAO.alterar(alterada);
        return reservaDAO.buscarPorCodigo(alterada.getCodigo()).orElseThrow();
    }

    /**
     * UC09: marca a reserva como CANCELADA sem apagar a linha. Não trava o tipo de quarto porque o
     * cancelamento só devolve capacidade — nenhuma operação simultânea fica com a conta errada por
     * ver a vaga ainda ocupada, e travar a reserva basta para não desfazer uma conversão em curso.
     */
    @Transactional
    public Reserva cancelar(Reserva selecionada) {
        Reserva gravada = exigirInalterada(selecionada, "confirme o cancelamento de novo");
        gravada.cancelar();
        reservaDAO.atualizar(gravada);
        return reservaDAO.buscarPorCodigo(gravada.getCodigo()).orElseThrow();
    }

    /**
     * Trava a linha da reserva e recusa se ela mudou — inclusive por um check-in que a converteu —
     * depois que a tela a localizou. É o que impede perda de atualização: a gravação só continua
     * sobre o estado que o usuário viu.
     */
    private Reserva exigirInalterada(Reserva selecionada, String acao) {
        Reserva gravada = reservaDAO.buscarPorCodigoParaAtualizar(selecionada.getCodigo())
                .orElseThrow(() -> inexistente(selecionada.getCodigo()));
        if (!gravada.mesmoEstadoDe(selecionada))
            throw new NegocioException("A reserva " + gravada.getCodigo() + " foi alterada por outra operação. "
                    + "Localize-a de novo e " + acao + ".");
        return gravada;
    }

    private TipoQuarto buscarTipo(String codigoTipo) {
        return tipoDAO.buscarPorCodigo(codigoTipo)
                .orElseThrow(() -> new NegocioException("Tipo de quarto não cadastrado: " + codigoTipo));
    }

    private static NegocioException inexistente(int codigo) {
        return new NegocioException("Reserva não localizada: " + codigo + ".");
    }
}
