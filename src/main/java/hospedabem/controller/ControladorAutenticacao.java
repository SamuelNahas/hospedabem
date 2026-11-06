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

    /** Inicia a sessão; com senha vencida, só a troca de senha é liberada até a renovação. */
    public Funcionario autenticar(String matricula, char[] senha) {
        ServicoAutenticacao.Acesso acesso = servico.autenticar(matricula, senha);
        sessao.iniciar(acesso.funcionario(), acesso.senhaVencida());
        return acesso.funcionario();
    }

    public boolean senhaVencida() {
        sessao.exigirAutenticado();
        return sessao.isSenhaVencida();
    }

    /** RNF-8: troca da própria senha, permitida também com a senha vencida. */
    public void trocarSenha(char[] atual, char[] nova, char[] confirmacao) {
        Funcionario f = sessao.exigirAutenticado();
        servico.trocarSenha(f.id(), atual, nova, confirmacao);
        sessao.senhaRenovada();
    }

    public void sair() {
        sessao.encerrar();
    }
}
