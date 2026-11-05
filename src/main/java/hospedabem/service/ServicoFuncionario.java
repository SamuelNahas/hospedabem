package hospedabem.service;

import hospedabem.dao.FuncionarioDAO;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import hospedabem.model.PoliticaDeSenha;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * UC02 Gerenciar Funcionário. A senha nunca é guardada: só o hash produzido por {@link Senha}.
 *
 * <p>As gravações usam {@link TransactionTemplate} em vez de {@code @Transactional} porque o hash
 * PBKDF2 custa 210 000 iterações e não pode ser calculado com uma conexão do pool presa na mão.
 * Toda gravação começa travando os administradores e reconferindo o autor no banco: a sessão guarda
 * o funcionário de quando ele entrou, e esse perfil pode ter sido alterado por outra estação.
 */
@Service
public class ServicoFuncionario {
    private final FuncionarioDAO dao;
    private final Clock relogio;
    private final TransactionTemplate transacao;

    public ServicoFuncionario(FuncionarioDAO dao, Clock relogio, PlatformTransactionManager gerenciador) {
        this.dao = dao;
        this.relogio = relogio;
        this.transacao = new TransactionTemplate(gerenciador);
    }

    @Transactional(readOnly = true)
    public List<Funcionario> listar() {
        return dao.listar();
    }

    @Transactional(readOnly = true)
    public List<Funcionario> procurar(String termo) {
        return dao.procurar(termo);
    }

    public Funcionario incluir(Funcionario informado, char[] senha, Funcionario autor) {
        try {
            Funcionario f = informado.validado();
            f.exigirContratacaoAte(LocalDate.now(relogio));
            PoliticaDeSenha.exigirValida(senha);
            String hash = Senha.gerarHash(senha);
            return traduzindoDuplicidade(f, () -> transacao.execute(status -> {
                exigirAutorAdministrador(autor);
                int id = dao.inserir(f, hash);
                return new Funcionario(id, f.matricula(), f.cpf(), f.nome(), f.endereco(), f.cidade(), f.estado(),
                        f.telefone(), f.email(), f.dataContratacao(), f.perfil());
            }));
        } finally {
            apagar(senha);
        }
    }

    public void alterar(Funcionario informado, Funcionario autor) {
        Funcionario f = informado.validado();
        f.exigirContratacaoAte(LocalDate.now(relogio));
        traduzindoDuplicidade(f, () -> transacao.execute(status -> {
            Funcionario autorAtual = exigirAutorAdministrador(autor);
            Funcionario gravado = exigirGravado(f.id());
            f.exigirAlteracaoPor(autorAtual, gravado.perfil());
            dao.atualizar(f);
            return null;
        }));
    }

    public void excluir(Funcionario f, Funcionario autor) {
        transacao.execute(status -> {
            Funcionario autorAtual = exigirAutorAdministrador(autor);
            f.exigirExclusaoPor(autorAtual);
            Funcionario gravado = exigirGravado(f.id());
            int vinculos = dao.contarVinculos(f.id());
            if (vinculos > 0)
                throw new NegocioException("O funcionário " + gravado.matricula() + " não pode ser excluído: há "
                        + vinculos + " reserva(s)/hospedagem(ns) registrada(s) por ele. O histórico é preservado; "
                        + "altere o perfil ou os dados em vez de excluir.");
            try {
                dao.excluir(f.id());
            } catch (DataIntegrityViolationException e) {
                throw new NegocioException("O funcionário " + gravado.matricula()
                        + " não pode ser excluído: há registros vinculados a ele.");
            }
            return null;
        });
    }

    /** Define uma nova senha para o funcionário e marca a data da troca (ponto de partida do RNF-8). */
    public void redefinirSenha(int id, char[] senha, Funcionario autor) {
        try {
            PoliticaDeSenha.exigirValida(senha);
            String hash = Senha.gerarHash(senha);
            transacao.execute(status -> {
                exigirAutorAdministrador(autor);
                if (dao.atualizarSenha(id, hash) == 0)
                    throw new NegocioException("Funcionário não encontrado; a lista pode estar desatualizada.");
                return null;
            });
        } finally {
            apagar(senha);
        }
    }

    /**
     * Trava as linhas dos administradores e confere no banco que o autor ainda existe e ainda é
     * administrador. Essa é a regra que garante que o hotel nunca fica sem nenhum administrador:
     * quem remove ou rebaixa um administrador é sempre outro administrador conferido agora, e
     * ninguém se exclui nem se rebaixa, então sempre sobra pelo menos o autor. A trava faz com que
     * duas estações que se rebaixam uma à outra ao mesmo tempo sejam atendidas em sequência, e a
     * segunda encontre o próprio perfil já rebaixado.
     */
    private Funcionario exigirAutorAdministrador(Funcionario autor) {
        dao.bloquearAdministradores();
        return dao.buscarPorId(autor.id()).filter(Funcionario::ehAdministrador)
                .orElseThrow(() -> new NegocioException("Sua sessão não tem mais perfil de administrador: o seu "
                        + "cadastro foi alterado ou excluído por outro administrador. Faça login novamente."));
    }

    private Funcionario exigirGravado(int id) {
        return dao.buscarPorId(id)
                .orElseThrow(() -> new NegocioException("Funcionário não encontrado; a lista pode estar desatualizada."));
    }

    /** Aponta qual campo repetiu, para a mensagem da tela dizer o que corrigir. */
    private void exigirSemDuplicidade(Funcionario f) {
        if (dao.existeMatricula(f.matricula(), f.id()))
            throw new NegocioException("Já existe funcionário com a matrícula " + f.matricula() + ".");
        if (dao.existeCpf(f.cpf(), f.id()))
            throw new NegocioException("Já existe funcionário com o CPF " + f.cpf() + ".");
    }

    /**
     * A matrícula e o CPF repetidos são barrados pelas restrições {@code UNIQUE} do banco, que valem
     * também entre estações simultâneas; aqui a violação é traduzida na mensagem que diz qual campo
     * repetiu. A releitura é feita fora da transação, porque a que falhou já foi abortada e não
     * aceita mais consultas. Visível no pacote porque a corrida entre duas estações não se reproduz
     * num teste de integração: ela é exercitada direto em {@code ServicoFuncionarioTest}.
     */
    <T> T traduzindoDuplicidade(Funcionario f, Supplier<T> gravacao) {
        try {
            return gravacao.get();
        } catch (DuplicateKeyException e) {
            exigirSemDuplicidade(f);
            throw new NegocioException("Já existe funcionário com a matrícula " + f.matricula()
                    + " ou com o CPF " + f.cpf() + ".");
        }
    }

    private static void apagar(char[] senha) {
        if (senha != null) Arrays.fill(senha, '\0');
    }
}
