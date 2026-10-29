package hospedabem.controller;

import hospedabem.dao.DisponibilidadeDAO;
import hospedabem.model.Disponibilidade;
import hospedabem.model.NegocioException;
import hospedabem.model.Ocupacao;
import hospedabem.model.Periodo;
import hospedabem.model.Reserva;
import hospedabem.model.TipoQuarto;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class ServicoDisponibilidade {
    private final DisponibilidadeDAO dao;
    private final Clock relogio;

    public ServicoDisponibilidade(DisponibilidadeDAO dao, Clock relogio) {
        this.dao = dao;
        this.relogio = relogio;
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
