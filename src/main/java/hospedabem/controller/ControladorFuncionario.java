package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import hospedabem.service.ServicoFuncionario;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * UC02 Gerenciar Funcionário. Toda operação exige sessão de administrador (RNF-10), inclusive a
 * delegação do perfil de administrador: chamadas sem sessão ou de recepcionista são recusadas aqui,
 * antes de chegar ao service.
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

    public Optional<Funcionario> buscar(String matricula) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.buscar(matricula);
    }

    public List<Funcionario> procurar(String termo) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.procurar(termo);
    }

    public Funcionario incluir(Funcionario f, char[] senha) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.incluir(f, senha);
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
        sessao.exigir(Perfil.ADMINISTRADOR);
        servico.redefinirSenha(f.id(), senha);
    }
}
