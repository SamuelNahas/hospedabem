package hospedabem.controller;

import hospedabem.model.Hospede;
import hospedabem.model.Periodo;
import hospedabem.model.Reserva;
import hospedabem.service.ServicoReserva;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ControladorReserva {
    private final Sessao sessao;
    private final ServicoReserva servico;

    public ControladorReserva(Sessao sessao, ServicoReserva servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public Hospede buscarResponsavel(String cpfOuPassaporte) {
        sessao.exigirQualquerPerfil();
        return servico.buscarResponsavel(cpfOuPassaporte);
    }

    public Reserva registrar(String cpfResponsavel, String codigoTipo, LocalDate entrada, LocalDate saida, int quantidade) {
        int funcionarioId = sessao.exigirQualquerPerfil().id();
        return servico.registrar(funcionarioId, cpfResponsavel, codigoTipo, new Periodo(entrada, saida), quantidade);
    }
}
