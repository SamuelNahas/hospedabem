package hospedabem.controller;

import hospedabem.model.Consumo;
import hospedabem.model.Hospedagem;
import hospedabem.model.ItemConsumo;
import hospedabem.model.Perfil;
import hospedabem.model.ResumoHospedagem;
import hospedabem.service.ServicoConsumo;
import org.springframework.stereotype.Component;

import java.util.List;

/** UC11 Lançar Consumo/Serviço: recepcionista e administrador (RNF-10). */
@Component
public class ControladorConsumo {
    private final Sessao sessao;
    private final ServicoConsumo servico;

    public ControladorConsumo(Sessao sessao, ServicoConsumo servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public List<ResumoHospedagem> listarHospedagensAtivas() {
        sessao.exigir(Perfil.RECEPCIONISTA, Perfil.ADMINISTRADOR);
        return servico.listarHospedagensAtivas();
    }

    public List<ItemConsumo> listarItens() {
        sessao.exigir(Perfil.RECEPCIONISTA, Perfil.ADMINISTRADOR);
        return servico.listarItens();
    }

    public Hospedagem localizar(int codigoHospedagem) {
        sessao.exigir(Perfil.RECEPCIONISTA, Perfil.ADMINISTRADOR);
        return servico.localizar(codigoHospedagem);
    }

    public Consumo lancar(int codigoHospedagem, String codigoItem, int quantidade) {
        int funcionario = sessao.exigir(Perfil.RECEPCIONISTA, Perfil.ADMINISTRADOR).id();
        return servico.lancar(codigoHospedagem, codigoItem, quantidade, funcionario);
    }
}
