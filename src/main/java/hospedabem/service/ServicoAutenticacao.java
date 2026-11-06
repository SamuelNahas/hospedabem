package hospedabem.service;

import hospedabem.dao.FuncionarioDAO;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class ServicoAutenticacao {
    private static final String HASH_FICTICIO = Senha.gerarHash("indisponivel".toCharArray());

    private final FuncionarioDAO funcionarioDAO;

    public ServicoAutenticacao(FuncionarioDAO funcionarioDAO) {
        this.funcionarioDAO = funcionarioDAO;
    }

    public Funcionario autenticar(String matricula, char[] senha) {
        try {
            if (matricula == null || matricula.isBlank() || senha == null || senha.length == 0)
                throw new NegocioException("Informe matrícula e senha.");
            var credencial = funcionarioDAO.buscarCredencial(matricula.trim());
            // Calcula o hash mesmo sem matrícula, para não revelar pelo tempo quais matrículas existem.
            boolean confere = Senha.confere(senha, credencial.map(FuncionarioDAO.Credencial::senhaHash).orElse(HASH_FICTICIO));
            if (credencial.isEmpty() || !confere) throw new NegocioException("Matrícula ou senha inválida.");
            return credencial.get().funcionario();
        } finally {
            if (senha != null) Arrays.fill(senha, '\0');
        }
    }
}
