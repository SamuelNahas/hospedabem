package hospedabem.controller;

import hospedabem.model.Disponibilidade;
import hospedabem.model.Periodo;
import hospedabem.model.TipoQuarto;
import hospedabem.service.ServicoDisponibilidade;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ControladorDisponibilidade {
    private final Sessao sessao;
    private final ServicoDisponibilidade servico;

    public ControladorDisponibilidade(Sessao sessao, ServicoDisponibilidade servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public List<Disponibilidade> consultar(LocalDate entrada, LocalDate saida, String codigoTipo) {
        sessao.exigirQualquerPerfil();
        return servico.consultar(new Periodo(entrada, saida), codigoTipo);
    }

    public List<TipoQuarto> listarTipos() {
        sessao.exigirQualquerPerfil();
        return servico.listarTipos();
    }
}
