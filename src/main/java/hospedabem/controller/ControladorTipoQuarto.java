package hospedabem.controller;

import hospedabem.model.Perfil;
import hospedabem.model.TipoQuarto;
import hospedabem.service.ServicoTipoQuarto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ControladorTipoQuarto {
    private final Sessao sessao;
    private final ServicoTipoQuarto servico;

    public ControladorTipoQuarto(Sessao sessao, ServicoTipoQuarto servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public List<TipoQuarto> listar() {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.listar();
    }

    public Optional<TipoQuarto> buscar(String codigo) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.buscar(codigo);
    }

    public TipoQuarto incluir(TipoQuarto t) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.incluir(t);
    }

    public void alterar(TipoQuarto t) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        servico.alterar(t);
    }

    public void excluir(TipoQuarto t) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        servico.excluir(t);
    }
}
