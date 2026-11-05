package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import hospedabem.service.ServicoFuncionario;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * UC02 Gerenciar Funcionário. Toda operação exige sessão de administrador (RNF-10), inclusive a
 * delegação do perfil de administrador: chamadas sem sessão ou de recepcionista são recusadas aqui,
 * antes de chegar ao service. O funcionário da sessão é repassado como autor, e o service o
 * reconfere no banco, porque o perfil pode ter mudado depois do login.
 */
@Component
public class ControladorFuncionario {
    private final Sessao sessao;
    private final ServicoFuncionario servico;

    public ControladorFuncionario(Sessao sessao, ServicoFuncionario servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public List<Funcionario> listar() {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.listar();
    }

    public List<Funcionario> procurar(String termo) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.procurar(termo);
    }

    public Funcionario incluir(Funcionario f, char[] senha) {
        Funcionario autor = sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.incluir(f, senha, autor);
    }

    public void alterar(Funcionario f) {
        Funcionario autor = sessao.exigir(Perfil.ADMINISTRADOR);
        servico.alterar(f, autor);
    }

    public void excluir(Funcionario f) {
        Funcionario autor = sessao.exigir(Perfil.ADMINISTRADOR);
        servico.excluir(f, autor);
    }

    public void redefinirSenha(Funcionario f, char[] senha) {
        Funcionario autor = sessao.exigir(Perfil.ADMINISTRADOR);
        servico.redefinirSenha(f.id(), senha, autor);
    }
}
