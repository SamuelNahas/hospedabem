package hospedabem.service;

import hospedabem.dao.FuncionarioDAO;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import hospedabem.model.PoliticaDeSenha;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ServicoAutenticacao {
    private static final String HASH_FICTICIO = Senha.gerarHash("indisponivel".toCharArray());

    private final FuncionarioDAO funcionarioDAO;
    private final Clock relogio;

    public ServicoAutenticacao(FuncionarioDAO funcionarioDAO, Clock relogio) {
        this.funcionarioDAO = funcionarioDAO;
        this.relogio = relogio;
    }

    /** Resultado do login: o funcionário e quando a senha dele vence (RNF-8). */
    public record Acesso(Funcionario funcionario, LocalDateTime senhaVenceEm) {
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
            return new Acesso(c.funcionario(), PoliticaDeSenha.venceEm(c.senhaAlteradaEm(), c.senhaProvisoria()));
        } finally {
            Senha.apagar(senha);
        }
    }

    /**
     * Troca da própria senha: valida confirmação e composição, confere a senha atual e grava só o
     * hash, marcando {@code senha_alterada_em}. A gravação é um único UPDATE, que só acontece se o
     * hash no banco ainda for o que foi conferido. Devolve o novo vencimento.
     */
    public LocalDateTime trocarSenha(int funcionarioId, char[] atual, char[] nova, char[] confirmacao) {
        try {
            PoliticaDeSenha.exigirTroca(atual, nova, confirmacao);
            String hashAtual = funcionarioDAO.buscarHash(funcionarioId)
                    .orElseThrow(() -> new NegocioException("Funcionário não encontrado. Faça login novamente."));
            if (!Senha.confere(atual, hashAtual)) throw new NegocioException("Senha atual incorreta.");
            String novoHash = Senha.gerarHash(nova);
            LocalDateTime agora = LocalDateTime.now(relogio);
            if (funcionarioDAO.trocarSenhaPropria(funcionarioId, hashAtual, novoHash, agora) == 0)
                throw new NegocioException("A senha foi alterada por outra operação. Tente novamente.");
            return PoliticaDeSenha.venceEm(agora, false);
        } finally {
            Senha.apagar(atual);
            Senha.apagar(nova);
            Senha.apagar(confirmacao);
        }
    }
}
