package hospedabem.controller;

import hospedabem.model.Hospede;
import hospedabem.model.Periodo;
import hospedabem.model.Reserva;
import hospedabem.service.ServicoReserva;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * UC08 Registrar Reserva e UC09 Consultar, Alterar e Cancelar Reserva. Toda operação exige sessão de
 * recepcionista ou de administrador (RNF-10): chamadas sem sessão são recusadas aqui, antes do service.
 */
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

    public Reserva consultar(int codigo) {
        sessao.exigirQualquerPerfil();
        return servico.consultar(codigo);
    }

    public Reserva alterar(Reserva selecionada, String cpfResponsavel, String codigoTipo, LocalDate entrada,
                           LocalDate saida, int quantidade) {
        sessao.exigirQualquerPerfil();
        return servico.alterar(selecionada, cpfResponsavel, codigoTipo, new Periodo(entrada, saida), quantidade);
    }

    public Reserva cancelar(Reserva selecionada) {
        sessao.exigirQualquerPerfil();
        return servico.cancelar(selecionada);
    }
}
