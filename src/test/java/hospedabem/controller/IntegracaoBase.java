package hospedabem.controller;

import hospedabem.dao.FuncionarioDAO;
import hospedabem.dao.InicializadorBanco;
import hospedabem.model.Hospedagem;
import hospedabem.model.Perfil;
import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import hospedabem.model.TipoQuarto;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Testes de integração contra o PostgreSQL de teste (application-test.properties). */
@SpringBootTest
@ActiveProfiles("test")
abstract class IntegracaoBase {
    static final String CARLOS = "11111111111";
    static final String BEATRIZ = "22222222222";
    static final String DIEGO = "33333333333";
    static final String ELISA = "44444444444";
    static final LocalDate HOJE = LocalDate.now();

    @Autowired JdbcTemplate jdbc;
    @Autowired Sessao sessao;
    @Autowired FuncionarioDAO funcionarioDAO;
    @Autowired InicializadorBanco inicializador;
    @Autowired ControladorAutenticacao autenticacao;
    @Autowired ControladorTipoQuarto tipos;
    @Autowired ControladorQuarto quartos;
    @Autowired ControladorDisponibilidade disponibilidade;
    @Autowired ControladorReserva reservas;
    @Autowired ObjectProvider<ControladorCheckIn> checkIns;

    @BeforeEach
    void recriarBancoDeTeste() {
        String banco = jdbc.queryForObject("SELECT current_database()", String.class);
        if (!banco.endsWith("_test"))
            throw new IllegalStateException("Recusado: testes só recriam bancos *_test, não " + banco);
        jdbc.execute("DROP SCHEMA public CASCADE");
        jdbc.execute("CREATE SCHEMA public");
        inicializador.inicializarSeVazio();
        logarComo("admin01");
    }

    void logarComo(String matricula) {
        sessao.iniciar(funcionarioDAO.buscarCredencial(matricula).orElseThrow().funcionario());
    }

    TipoQuarto criarTipo(String codigo, int capacidade) {
        return tipos.incluir(new TipoQuarto(0, codigo, "Tipo " + codigo, capacidade, new BigDecimal("100.00")));
    }

    Quarto criarQuarto(int numero, TipoQuarto tipo) {
        return quartos.incluir(new Quarto(0, numero, numero / 100, tipo, SituacaoQuarto.DISPONIVEL));
    }

    /** Check-in sem reserva completo, do início à confirmação. */
    Hospedagem checkInSemReserva(TipoQuarto tipo, int numeroQuarto, int dias, String... documentos) {
        ControladorCheckIn c = checkIns.getObject();
        c.iniciarCheckInSemReserva(documentos[0], tipo.codigo(), HOJE, HOJE.plusDays(dias));
        for (String d : documentos) c.adicionarHospede(d);
        c.selecionarQuarto(numeroQuarto);
        return c.confirmarCheckIn();
    }

    int contar(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    String situacaoQuarto(int numero) {
        return jdbc.queryForObject("SELECT situacao FROM quarto WHERE numero = ?", String.class, numero);
    }

    String situacaoReserva(int codigo) {
        return jdbc.queryForObject("SELECT situacao FROM reserva WHERE codigo = ?", String.class, codigo);
    }

    Perfil perfilDaSessao() {
        return sessao.getFuncionario().perfil();
    }
}
