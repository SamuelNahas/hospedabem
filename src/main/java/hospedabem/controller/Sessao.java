package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** Funcionário autenticado nesta instância do desktop (um usuário por processo). */
@Component
public class Sessao {
    private volatile Funcionario funcionario;

    public Funcionario getFuncionario() {
        return funcionario;
    }

    void iniciar(Funcionario f) {
        funcionario = f;
    }

    public void encerrar() {
        funcionario = null;
    }

    /** Verificação feita no controlador, independente do que a tela habilita (RF-2 a RF-7, RNF-10). */
    public Funcionario exigir(Perfil... permitidos) {
        Funcionario f = funcionario;
        if (f == null) throw new AcessoNegadoException("Acesso não autorizado: faça login.");
        if (!Arrays.asList(permitidos).contains(f.perfil()))
            throw new AcessoNegadoException("Permissão insuficiente: operação restrita a " + Arrays.toString(permitidos) + ".");
        return f;
    }

    public Funcionario exigirQualquerPerfil() {
        return exigir(Perfil.values());
    }
}
