package hospedabem.service;

import hospedabem.dao.DisponibilidadeDAO;
import hospedabem.dao.TipoQuartoDAO;
import hospedabem.model.Disponibilidade;
import hospedabem.model.NegocioException;
import hospedabem.model.Ocupacao;
import hospedabem.model.Periodo;
import hospedabem.model.Reserva;
import hospedabem.model.TipoQuarto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class ServicoDisponibilidade {
    private final DisponibilidadeDAO dao;
    private final TipoQuartoDAO tipoDAO;
    private final JdbcTemplate jdbc;
    private final Clock relogio;

    public ServicoDisponibilidade(DisponibilidadeDAO dao, TipoQuartoDAO tipoDAO, JdbcTemplate jdbc, Clock relogio) {
        this.dao = dao;
        this.tipoDAO = tipoDAO;
        this.jdbc = jdbc;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<Disponibilidade> consultar(Periodo periodo, String codigoTipo) {
        jdbc.execute("SET LOCAL statement_timeout = 3000");
        List<TipoQuarto> tipos = codigoTipo == null ? tipoDAO.listar()
                : List.of(tipoDAO.buscarPorCodigo(codigoTipo)
                .orElseThrow(() -> new NegocioException("Tipo de quarto não cadastrado: " + codigoTipo)));
        return tipos.stream().map(t -> calcular(t, periodo)).toList();
    }

    @Transactional(readOnly = true)
    public List<TipoQuarto> listarTipos() {
        return tipoDAO.listar();
    }

    public Disponibilidade calcular(TipoQuarto tipo, Periodo periodo) {
        return calcular(tipo, periodo, null);
    }

    public Disponibilidade calcular(TipoQuarto tipo, Periodo periodo, Reserva emConversao) {
        int ignorada = emConversao == null ? -1 : emConversao.getId();
        int pico = Ocupacao.pico(dao.estadias(tipo.id(), periodo, ignorada, LocalDate.now(relogio)), periodo);
        return new Disponibilidade(tipo, dao.quartosOperacionais(tipo.id()), pico);
    }

    public void garantirCompromissos(TipoQuarto tipo) {
        LocalDate hoje = LocalDate.now(relogio);
        dao.fimDosCompromissos(tipo.id(), hoje).ifPresent(fim -> {
            Disponibilidade d = calcular(tipo, new Periodo(hoje, fim));
            if (d.picoOcupacao() > d.quartosOperacionais())
                throw new NegocioException("Operação recusada: o tipo " + tipo.codigo() + " ficaria com "
                        + d.quartosOperacionais() + " quarto(s) para " + d.picoOcupacao()
                        + " reserva(s)/hospedagem(ns) simultâneas já registradas.");
        });
    }
}
