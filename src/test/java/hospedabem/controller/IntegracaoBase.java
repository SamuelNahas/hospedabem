package hospedabem.controller;

import hospedabem.BancoDeTeste;
import hospedabem.dao.FuncionarioDAO;
import hospedabem.dao.InicializadorBanco;
import hospedabem.model.Funcionario;
import hospedabem.model.Hospedagem;
import hospedabem.model.Perfil;
import hospedabem.model.Quarto;
import hospedabem.model.SituacaoQuarto;
import hospedabem.model.TipoQuarto;
import hospedabem.service.ServicoFuncionario;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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
    @Autowired ControladorFuncionario funcionarios;
    // Usado só onde duas sessões simultâneas são necessárias: a Sessao é única por aplicação.
    @Autowired ServicoFuncionario servicoFuncionario;
    @Autowired ControladorTipoQuarto tipos;
    @Autowired ControladorQuarto quartos;
    @Autowired ControladorDisponibilidade disponibilidade;
    @Autowired ControladorReserva reservas;
    @Autowired ObjectProvider<ControladorCheckIn> checkIns;

    @BeforeEach
    void recriarBancoDeTeste() {
        BancoDeTeste.recriar(jdbc, inicializador);
        logarComo("admin01");
    }

    void logarComo(String matricula) {
        sessao.iniciar(funcionarioDAO.buscarCredencial(matricula).orElseThrow().funcionario());
    }

    /** Funcionário válido com os campos obrigatórios; o id 0 indica inclusão. */
    Funcionario novoFuncionario(String matricula, String cpf, Perfil perfil) {
        return Funcionario.informado(0, matricula, cpf, "Marta Teste " + matricula, "Rua Fictícia, 1",
                "Campo Grande", "MS", "(67) 90000-0009", matricula + "@exemplo.test", HOJE.minusYears(1), perfil);
    }

    /** Funcionário de matrícula exata, pela mesma busca que a tela usa. */
    Funcionario funcionario(String matricula) {
        return acharFuncionario(matricula).orElseThrow();
    }

    Optional<Funcionario> acharFuncionario(String matricula) {
        String m = matricula.trim().toLowerCase();
        return funcionarios.procurar(m).stream().filter(f -> f.matricula().equals(m)).findFirst();
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
