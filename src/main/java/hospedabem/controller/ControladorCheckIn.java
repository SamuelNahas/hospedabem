package hospedabem.controller;

import hospedabem.model.Atendimento;
import hospedabem.model.Hospedagem;
import hospedabem.model.Hospede;
import hospedabem.model.NegocioException;
import hospedabem.model.Periodo;
import hospedabem.model.Quarto;
import hospedabem.model.Reserva;
import hospedabem.service.ServicoCheckIn;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ControladorCheckIn {
    private final Sessao sessao;
    private final ServicoCheckIn servico;
    private Atendimento atendimento;

    public ControladorCheckIn(Sessao sessao, ServicoCheckIn servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public Reserva iniciarCheckIn(int codReserva) {
        sessao.exigirQualquerPerfil();
        atendimento = null;
        atendimento = servico.iniciarComReserva(codReserva);
        return atendimento.getReserva();
    }

    public Atendimento iniciarCheckInSemReserva(String cpfResponsavel, String codTipo, LocalDate entrada, LocalDate saida) {
        sessao.exigirQualquerPerfil();
        atendimento = null;
        atendimento = servico.iniciarSemReserva(cpfResponsavel, codTipo, new Periodo(entrada, saida));
        return atendimento;
    }

    public Hospede adicionarHospede(String cpfOuPassaporte) {
        sessao.exigirQualquerPerfil();
        return servico.adicionarHospede(getAtendimento(), cpfOuPassaporte);
    }

    public void removerHospede(Hospede h) {
        getAtendimento().remover(h);
    }

    public List<Quarto> consultarQuartosDisponiveis() {
        sessao.exigirQualquerPerfil();
        return servico.consultarQuartosDisponiveis(getAtendimento());
    }

    public Atendimento selecionarQuarto(int numero) {
        sessao.exigirQualquerPerfil();
        servico.selecionarQuarto(getAtendimento(), numero);
        return atendimento;
    }

    public Hospedagem confirmarCheckIn() {
        int funcionarioId = sessao.exigirQualquerPerfil().id();
        Hospedagem h = servico.confirmar(getAtendimento(), funcionarioId);
        atendimento = null;
        return h;
    }

    public Atendimento getAtendimento() {
        if (atendimento == null) throw new NegocioException("Inicie o check-in com uma reserva ou sem reserva.");
        return atendimento;
    }
}
