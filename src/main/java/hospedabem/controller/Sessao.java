package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class Sessao {
    private volatile Funcionario funcionario;
    private volatile boolean senhaVencida;

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public boolean isSenhaVencida() {
        return senhaVencida;
    }

    void iniciar(Funcionario f) {
        iniciar(f, false);
    }

    void iniciar(Funcionario f, boolean vencida) {
        senhaVencida = vencida;
        funcionario = f;
    }

    void senhaRenovada() {
        senhaVencida = false;
    }

    public void encerrar() {
        funcionario = null;
        senhaVencida = false;
    }

    /** Exige sessão com um dos perfis e senha em dia (RNF-8): senha vencida só permite a troca. */
    public Funcionario exigir(Perfil... permitidos) {
        Funcionario f = exigirAutenticado();
        if (senhaVencida)
            throw new AcessoNegadoException("Senha vencida: troque a sua senha para continuar.");
        if (!Arrays.asList(permitidos).contains(f.perfil()))
            throw new AcessoNegadoException("Permissão insuficiente: operação restrita a " + Arrays.toString(permitidos) + ".");
        return f;
    }

    public Funcionario exigirQualquerPerfil() {
        return exigir(Perfil.values());
    }

    /** Só exige login, mesmo com a senha vencida: usado pela troca de senha. */
    Funcionario exigirAutenticado() {
        Funcionario f = funcionario;
        if (f == null) throw new AcessoNegadoException("Acesso não autorizado: faça login.");
        return f;
    }
}
