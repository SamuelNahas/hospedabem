package hospedabem.controller;

import hospedabem.model.Perfil;
import hospedabem.model.Quarto;
import hospedabem.service.ServicoQuarto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ControladorQuarto {
    private final Sessao sessao;
    private final ServicoQuarto servico;

    public ControladorQuarto(Sessao sessao, ServicoQuarto servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public List<Quarto> listar() {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.listar();
    }

    public Quarto incluir(Quarto q) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.incluir(q);
    }

    public void alterar(Quarto novo) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        servico.alterar(novo);
    }

    public void excluir(Quarto q) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        servico.excluir(q);
    }
}
