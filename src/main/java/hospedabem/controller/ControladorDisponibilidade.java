package hospedabem.controller;

import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.Disponibilidade;
import hospedabem.model.NegocioException;
import hospedabem.model.Periodo;
import hospedabem.model.TipoQuarto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class ControladorDisponibilidade {
    private final Sessao sessao;
    private final TipoQuartoDAO tipoDAO;
    private final ServicoDisponibilidade servico;
    private final JdbcTemplate jdbc;

    public ControladorDisponibilidade(Sessao sessao, TipoQuartoDAO tipoDAO, ServicoDisponibilidade servico, JdbcTemplate jdbc) {
        this.sessao = sessao;
        this.tipoDAO = tipoDAO;
        this.servico = servico;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Disponibilidade> consultar(LocalDate entrada, LocalDate saida, String codigoTipo) {
        sessao.exigirQualquerPerfil();
        Periodo periodo = new Periodo(entrada, saida);
        jdbc.execute("SET LOCAL statement_timeout = 3000");
        List<TipoQuarto> tipos = codigoTipo == null ? tipoDAO.listar()
                : List.of(tipoDAO.buscarPorCodigo(codigoTipo)
                .orElseThrow(() -> new NegocioException("Tipo de quarto não cadastrado: " + codigoTipo)));
        return tipos.stream().map(t -> servico.calcular(t, periodo)).toList();
    }

    @Transactional(readOnly = true)
    public List<TipoQuarto> listarTipos() {
        sessao.exigirQualquerPerfil();
        return tipoDAO.listar();
    }
}
