package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.service.ServicoAutenticacao;
import org.springframework.stereotype.Component;

@Component
public class ControladorAutenticacao {
    private final ServicoAutenticacao servico;
    private final Sessao sessao;

    public ControladorAutenticacao(ServicoAutenticacao servico, Sessao sessao) {
        this.servico = servico;
        this.sessao = sessao;
    }

    public Funcionario autenticar(String matricula, char[] senha) {
        Funcionario f = servico.autenticar(matricula, senha);
        sessao.iniciar(f);
        return f;
    }

    public void sair() {
        sessao.encerrar();
    }
}
