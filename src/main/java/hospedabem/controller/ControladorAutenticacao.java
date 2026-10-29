package hospedabem.controller;

import hospedabem.dao.FuncionarioDAO;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** UC01 Autenticar-se. */
@Component
public class ControladorAutenticacao {
    private static final String HASH_FICTICIO = Senha.gerarHash("indisponivel".toCharArray());

    private final FuncionarioDAO funcionarioDAO;
    private final Sessao sessao;

    public ControladorAutenticacao(FuncionarioDAO funcionarioDAO, Sessao sessao) {
        this.funcionarioDAO = funcionarioDAO;
        this.sessao = sessao;
    }

    public Funcionario autenticar(String matricula, char[] senha) {
        try {
            if (matricula == null || matricula.isBlank() || senha == null || senha.length == 0)
                throw new NegocioException("Informe matrícula e senha.");
            var credencial = funcionarioDAO.buscarCredencial(matricula.trim());
            // Calcula o hash mesmo sem matrícula, para não revelar pelo tempo quais matrículas existem.
            boolean confere = Senha.confere(senha, credencial.map(FuncionarioDAO.Credencial::senhaHash).orElse(HASH_FICTICIO));
            if (credencial.isEmpty() || !confere) throw new NegocioException("Matrícula ou senha inválida.");
            sessao.iniciar(credencial.get().funcionario());
            return credencial.get().funcionario();
        } finally {
            if (senha != null) Arrays.fill(senha, '\0');
        }
    }

    public void sair() {
        sessao.encerrar();
    }
}
