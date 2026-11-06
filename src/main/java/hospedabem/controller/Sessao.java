package hospedabem.controller;

import hospedabem.model.Funcionario;
import hospedabem.model.Perfil;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class Sessao {
    private final Clock relogio;
    private volatile Funcionario funcionario;
    /** Quando a senha do funcionário vence; {@code null} quando a sessão não controla validade. */
    private volatile LocalDateTime senhaVenceEm;

    public Sessao(Clock relogio) {
        this.relogio = relogio;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    /** Conferido a cada operação: a senha que vence com a sessão aberta também bloqueia (RNF-8). */
    public boolean isSenhaVencida() {
        LocalDateTime vence = senhaVenceEm;
        return vence != null && !LocalDateTime.now(relogio).isBefore(vence);
    }

    void iniciar(Funcionario f) {
        iniciar(f, null);
    }

    void iniciar(Funcionario f, LocalDateTime venceEm) {
        senhaVenceEm = venceEm;
        funcionario = f;
    }

    void senhaRenovada(LocalDateTime venceEm) {
        senhaVenceEm = venceEm;
    }

    public void encerrar() {
        funcionario = null;
        senhaVenceEm = null;
    }

    /** Exige sessão com um dos perfis e senha em dia (RNF-8): senha vencida só permite a troca. */
    public Funcionario exigir(Perfil... permitidos) {
        Funcionario f = exigirAutenticado();
        if (isSenhaVencida())
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
