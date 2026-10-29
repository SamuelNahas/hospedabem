package hospedabem.controller;

import hospedabem.dao.DisponibilidadeDAO;
import hospedabem.model.NegocioException;
import hospedabem.model.Ocupacao;
import hospedabem.model.Periodo;
import hospedabem.model.TipoQuarto;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** Regra de disponibilidade compartilhada por consulta, reserva, check-in e cadastro de quartos. */
@Component
public class ServicoDisponibilidade {
    static final int NENHUMA_RESERVA = -1;

    private final DisponibilidadeDAO dao;
    private final Clock relogio;

    public ServicoDisponibilidade(DisponibilidadeDAO dao, Clock relogio) {
        this.dao = dao;
        this.relogio = relogio;
    }

    /** ignorarReservaId: reserva que está sendo convertida e não deve ser contada junto da nova hospedagem. */
    public Disponibilidade calcular(TipoQuarto tipo, Periodo periodo, int ignorarReservaId) {
        int pico = Ocupacao.pico(dao.estadias(tipo.id(), periodo, ignorarReservaId, LocalDate.now(relogio)), periodo);
        return new Disponibilidade(tipo, dao.quartosOperacionais(tipo.id()), pico);
    }

    /** RN-Q05/RN-Q06: os quartos restantes do tipo ainda comportam todos os compromissos futuros. */
    public void garantirCompromissos(TipoQuarto tipo) {
        LocalDate hoje = LocalDate.now(relogio);
        dao.fimDosCompromissos(tipo.id(), hoje).ifPresent(fim -> {
            Disponibilidade d = calcular(tipo, new Periodo(hoje, fim), NENHUMA_RESERVA);
            if (d.picoOcupacao() > d.quartosOperacionais())
                throw new NegocioException("Operação recusada: o tipo " + tipo.codigo() + " ficaria com "
                        + d.quartosOperacionais() + " quarto(s) para " + d.picoOcupacao()
                        + " reserva(s)/hospedagem(ns) simultâneas já registradas.");
        });
    }
}
