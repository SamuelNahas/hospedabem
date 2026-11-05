package hospedabem.controller;

import hospedabem.model.ItemConsumo;
import hospedabem.model.Perfil;
import hospedabem.service.ServicoItemConsumo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ControladorItemConsumo {
    private final Sessao sessao;
    private final ServicoItemConsumo servico;

    public ControladorItemConsumo(Sessao sessao, ServicoItemConsumo servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public List<ItemConsumo> listar() {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.listar();
    }

    public Optional<ItemConsumo> buscar(String codigo) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.buscar(codigo);
    }

    public ItemConsumo incluir(ItemConsumo item) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        return servico.incluir(item);
    }

    public void alterar(ItemConsumo item) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        servico.alterar(item);
    }

    public void excluir(ItemConsumo item) {
        sessao.exigir(Perfil.ADMINISTRADOR);
        servico.excluir(item);
    }
}
