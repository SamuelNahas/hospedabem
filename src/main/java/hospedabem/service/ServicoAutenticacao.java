package hospedabem.service;

import hospedabem.dao.FuncionarioDAO;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import hospedabem.model.PoliticaDeSenha;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class ServicoAutenticacao {
    private static final String HASH_FICTICIO = Senha.gerarHash("indisponivel".toCharArray());

    private final FuncionarioDAO funcionarioDAO;
    private final Clock relogio;
    private final TransactionTemplate transacao;

    public ServicoAutenticacao(FuncionarioDAO funcionarioDAO, Clock relogio, PlatformTransactionManager gerenciador) {
        this.funcionarioDAO = funcionarioDAO;
        this.relogio = relogio;
        this.transacao = new TransactionTemplate(gerenciador);
    }

    /** Resultado do login: o funcionário e se a senha precisa ser renovada antes de operar (RNF-8). */
    public record Acesso(Funcionario funcionario, boolean senhaVencida) {
    }

    public Acesso autenticar(String matricula, char[] senha) {
        try {
            if (matricula == null || matricula.isBlank() || senha == null || senha.length == 0)
                throw new NegocioException("Informe matrícula e senha.");
            var credencial = funcionarioDAO.buscarCredencial(matricula.trim());
            // Calcula o hash mesmo sem matrícula, para não revelar pelo tempo quais matrículas existem.
            boolean confere = Senha.confere(senha, credencial.map(FuncionarioDAO.Credencial::senhaHash).orElse(HASH_FICTICIO));
            if (credencial.isEmpty() || !confere) throw new NegocioException("Matrícula ou senha inválida.");
            FuncionarioDAO.Credencial c = credencial.get();
            return new Acesso(c.funcionario(),
                    PoliticaDeSenha.estaVencida(c.senhaAlteradaEm(), c.senhaProvisoria(), LocalDateTime.now(relogio)));
        } finally {
            apagar(senha);
        }
    }

    /**
     * Troca da própria senha: valida confirmação e composição, confere a senha atual e grava só o
     * hash, marcando {@code senha_alterada_em}. O hash PBKDF2 é calculado fora da transação; a
     * gravação só acontece se o hash no banco ainda for o que foi conferido.
     */
    public void trocarSenha(int funcionarioId, char[] atual, char[] nova, char[] confirmacao) {
        try {
            PoliticaDeSenha.exigirTroca(atual, nova, confirmacao);
            String hashAtual = funcionarioDAO.buscarHash(funcionarioId)
                    .orElseThrow(() -> new NegocioException("Funcionário não encontrado. Faça login novamente."));
            if (!Senha.confere(atual, hashAtual)) throw new NegocioException("Senha atual incorreta.");
            String novoHash = Senha.gerarHash(nova);
            LocalDateTime agora = LocalDateTime.now(relogio);
            transacao.executeWithoutResult(status -> {
                if (funcionarioDAO.trocarSenhaPropria(funcionarioId, hashAtual, novoHash, agora) == 0)
                    throw new NegocioException("A senha foi alterada por outra operação. Tente novamente.");
            });
        } finally {
            apagar(atual);
            apagar(nova);
            apagar(confirmacao);
        }
    }

    private static void apagar(char[] senha) {
        if (senha != null) Arrays.fill(senha, '\0');
    }
}
