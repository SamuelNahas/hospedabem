package hospedabem.controller;

import hospedabem.dao.HospedeDAO;
import hospedabem.dao.ReservaDAO;
import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.Funcionario;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import hospedabem.model.Periodo;
import hospedabem.model.Reserva;
import hospedabem.model.TipoQuarto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** UC08 Registrar Reserva (administrador e recepcionista, RF-6). */
@Component
public class ControladorReserva {
    private final Sessao sessao;
    private final HospedeDAO hospedeDAO;
    private final TipoQuartoDAO tipoDAO;
    private final ReservaDAO reservaDAO;
    private final ServicoDisponibilidade disponibilidade;
    private final Clock relogio;

    public ControladorReserva(Sessao sessao, HospedeDAO hospedeDAO, TipoQuartoDAO tipoDAO, ReservaDAO reservaDAO,
                              ServicoDisponibilidade disponibilidade, Clock relogio) {
        this.sessao = sessao;
        this.hospedeDAO = hospedeDAO;
        this.tipoDAO = tipoDAO;
        this.reservaDAO = reservaDAO;
        this.disponibilidade = disponibilidade;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Hospede buscarResponsavel(String cpfOuPassaporte) {
        sessao.exigirQualquerPerfil();
        return hospedeDAO.buscarPorDocumento(cpfOuPassaporte).orElseThrow(() -> new NegocioException(
                "Hóspede não cadastrado: " + cpfOuPassaporte + ". O cadastro de hóspedes (UC06) será entregue na "
                        + "iteração 2; nesta versão use os hóspedes da carga inicial."));
    }

    /** Revalida tudo, inclusive a disponibilidade, dentro da transação e com o tipo bloqueado. */
    @Transactional
    public Reserva registrar(String cpfResponsavel, String codigoTipo, LocalDate entrada, LocalDate saida, int quantidade) {
        Funcionario funcionario = sessao.exigirQualquerPerfil();
        Hospede responsavel = buscarResponsavel(cpfResponsavel);
        TipoQuarto tipo = tipoDAO.buscarPorCodigo(codigoTipo)
                .orElseThrow(() -> new NegocioException("Tipo de quarto não cadastrado: " + codigoTipo));
        Periodo periodo = new Periodo(entrada, saida);
        if (entrada.isBefore(LocalDate.now(relogio)))
            throw new NegocioException("A data de entrada não pode estar no passado.");
        if (quantidade <= 0) throw new NegocioException("Quantidade de hóspedes deve ser positiva.");
        if (quantidade > tipo.capacidadeMaxima())
            throw new NegocioException("Quantidade de hóspedes (" + quantidade + ") excede a capacidade do tipo "
                    + tipo.codigo() + " (" + tipo.capacidadeMaxima() + ").");
        tipoDAO.bloquear(List.of(tipo.id()));
        if (disponibilidade.calcular(tipo, periodo, ServicoDisponibilidade.NENHUMA_RESERVA).livres() < 1)
            throw new NegocioException("Não há quarto do tipo " + tipo.codigo() + " disponível no período informado.");
        int codigo = reservaDAO.inserir(responsavel.id(), tipo.id(), periodo, quantidade, funcionario.id());
        return reservaDAO.buscarPorCodigo(codigo).orElseThrow();
    }
}
