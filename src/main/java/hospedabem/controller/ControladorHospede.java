package hospedabem.controller;

import hospedabem.model.Hospede;
import hospedabem.model.Perfil;
import hospedabem.service.ServicoHospede;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * UC06 Gerenciar Hóspede. Toda operação exige sessão de recepcionista ou de administrador (RNF-10):
 * chamadas sem sessão são recusadas aqui, antes de chegar ao service. O cadastro de hóspede é parte
 * do atendimento, por isso não é restrito ao administrador como os cadastros do UC02 e do UC05.
 */
@Component
public class ControladorHospede {
    private static final Perfil[] PERMITIDOS = {Perfil.RECEPCIONISTA, Perfil.ADMINISTRADOR};

    private final Sessao sessao;
    private final ServicoHospede servico;

    public ControladorHospede(Sessao sessao, ServicoHospede servico) {
        this.sessao = sessao;
        this.servico = servico;
    }

    public List<Hospede> listar() {
        sessao.exigir(PERMITIDOS);
        return servico.listar();
    }

    public List<Hospede> procurar(String termo) {
        sessao.exigir(PERMITIDOS);
        return servico.procurar(termo);
    }

    public Optional<Hospede> buscar(String cpfOuPassaporte) {
        sessao.exigir(PERMITIDOS);
        return servico.buscarPorDocumento(cpfOuPassaporte);
    }

    public Hospede incluir(Hospede h) {
        sessao.exigir(PERMITIDOS);
        return servico.incluir(h);
    }

    public void alterar(Hospede h) {
        sessao.exigir(PERMITIDOS);
        servico.alterar(h);
    }

    public void excluir(Hospede h) {
        sessao.exigir(PERMITIDOS);
        servico.excluir(h);
    }
}
