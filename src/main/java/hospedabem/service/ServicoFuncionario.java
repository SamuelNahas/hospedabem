package hospedabem.service;

import hospedabem.dao.FuncionarioDAO;
import hospedabem.model.Funcionario;
import hospedabem.model.NegocioException;
import hospedabem.model.PoliticaDeSenha;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** UC02 Gerenciar Funcionário. A senha nunca é guardada: só o hash produzido por {@link Senha}. */
@Service
public class ServicoFuncionario {
    private final FuncionarioDAO dao;
    private final Clock relogio;

    public ServicoFuncionario(FuncionarioDAO dao, Clock relogio) {
        this.dao = dao;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<Funcionario> listar() {
        return dao.listar();
    }

    @Transactional(readOnly = true)
    public Optional<Funcionario> buscar(String matricula) {
        return dao.buscarPorMatricula(matricula);
    }

    @Transactional(readOnly = true)
    public List<Funcionario> procurar(String termo) {
        return dao.procurar(termo);
    }

    @Transactional
    public Funcionario incluir(Funcionario f, char[] senha) {
        try {
            f.exigirContratacaoAte(LocalDate.now(relogio));
            PoliticaDeSenha.exigirValida(senha);
            exigirSemDuplicidade(f);
            int id = dao.inserir(f, Senha.gerarHash(senha));
            return new Funcionario(id, f.matricula(), f.cpf(), f.nome(), f.endereco(), f.cidade(), f.estado(),
                    f.telefone(), f.email(), f.dataContratacao(), f.perfil());
        } finally {
            apagar(senha);
        }
    }

    @Transactional
    public void alterar(Funcionario f, Funcionario autor) {
        f.exigirContratacaoAte(LocalDate.now(relogio));
        Funcionario gravado = dao.buscarPorId(f.id())
                .orElseThrow(() -> new NegocioException("Funcionário não encontrado; a lista pode estar desatualizada."));
        f.exigirAlteracaoPor(autor, gravado.perfil());
        if (gravado.ehAdministrador() && !f.ehAdministrador()) exigirOutroAdministrador(f.id(), "rebaixar");
        exigirSemDuplicidade(f);
        dao.atualizar(f);
    }

    @Transactional
    public void excluir(Funcionario f, Funcionario autor) {
        f.exigirExclusaoPor(autor);
        Funcionario gravado = dao.buscarPorId(f.id())
                .orElseThrow(() -> new NegocioException("Funcionário não encontrado; a lista pode estar desatualizada."));
        if (gravado.ehAdministrador()) exigirOutroAdministrador(f.id(), "excluir");
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
    }

    /** Define uma nova senha para o funcionário e marca a data da troca (ponto de partida do RNF-8). */
    @Transactional
    public void redefinirSenha(int id, char[] senha) {
        try {
            PoliticaDeSenha.exigirValida(senha);
            if (dao.atualizarSenha(id, Senha.gerarHash(senha)) == 0)
                throw new NegocioException("Funcionário não encontrado; a lista pode estar desatualizada.");
        } finally {
            apagar(senha);
        }
    }

    private void exigirOutroAdministrador(int id, String acao) {
        if (dao.contarAdministradores(id) == 0)
            throw new NegocioException("Não é possível " + acao + " o último administrador: o sistema ficaria sem "
                    + "ninguém para gerenciar funcionários. Cadastre outro administrador primeiro.");
    }

    /**
     * Recusa antes de gravar, para apontar qual campo repetiu e deixar a mensagem compreensível na tela.
     * As restrições {@code UNIQUE} do banco continuam sendo a garantia final entre estações simultâneas.
     */
    private void exigirSemDuplicidade(Funcionario f) {
        if (dao.existeMatricula(f.matricula(), f.id()))
            throw new NegocioException("Já existe funcionário com a matrícula " + f.matricula() + ".");
        if (dao.existeCpf(f.cpf(), f.id()))
            throw new NegocioException("Já existe funcionário com o CPF " + f.cpf() + ".");
    }

    private static void apagar(char[] senha) {
        if (senha != null) Arrays.fill(senha, '\0');
    }
}
