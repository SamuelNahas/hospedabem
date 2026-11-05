package hospedabem.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC02: regras de formato, obrigatoriedade e políticas do funcionário, sem banco. */
class FuncionarioTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 5);

    private static Funcionario funcionario(int id, String matricula, String cpf, Perfil perfil) {
        return Funcionario.informado(id, matricula, cpf, "Marta Teste", null, null, null, null, null, HOJE, perfil);
    }

    private static Funcionario valido() {
        return funcionario(1, "marta01", "000.000.001-91", Perfil.RECEPCIONISTA);
    }

    @Test
    void camposObrigatoriosSaoNormalizadosEOsOpcionaisVaziosViramNulos() {
        Funcionario f = Funcionario.informado(7, "  MARTA01 ", "00000000191", "  Marta   Teste  Silva ", "  ", "",
                " ms ", " (67) 90000-0001 ", " marta@exemplo.test ", HOJE, Perfil.ADMINISTRADOR);
        assertEquals("marta01", f.matricula());
        assertEquals("000.000.001-91", f.cpf());
        assertEquals("Marta Teste Silva", f.nome());
        assertNull(f.endereco());
        assertNull(f.cidade());
        assertEquals("MS", f.estado());
        assertEquals("(67) 90000-0001", f.telefone());
        assertEquals("marta@exemplo.test", f.email());
        assertTrue(f.ehAdministrador());
        assertEquals("marta01 - Marta Teste Silva", f.toString());
    }

    @Test
    void matriculaObrigatoriaComTamanhoEConteudoValidos() {
        assertThrows(NegocioException.class, () -> funcionario(0, null, "000.000.001-91", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "   ", "000.000.001-91", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "ab", "000.000.001-91", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class,
                () -> funcionario(0, "a".repeat(21), "000.000.001-91", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "marta-01", "000.000.001-91", Perfil.RECEPCIONISTA));
        assertEquals("abc", funcionario(0, "abc", "000.000.001-91", Perfil.RECEPCIONISTA).matricula());
        assertEquals(20, funcionario(0, "a".repeat(20), "000.000.001-91", Perfil.RECEPCIONISTA).matricula().length());
    }

    @Test
    void cpfObrigatorioComOnzeDigitosEDigitosVerificadoresCorretos() {
        assertThrows(NegocioException.class, () -> funcionario(0, "marta01", null, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "marta01", "...-", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "marta01", "0000000019", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "marta01", "000000001911", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "marta01", "11111111111", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "marta01", "000.000.001-92", Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> funcionario(0, "marta01", "000.000.001-81", Perfil.RECEPCIONISTA));
        assertEquals("123.456.789-09", funcionario(0, "marta01", "12345678909", Perfil.RECEPCIONISTA).cpf());
        assertEquals("000.000.002-72", funcionario(0, "marta01", "00000000272", Perfil.RECEPCIONISTA).cpf());
    }

    @Test
    void nomeObrigatorioComTamanhoValido() {
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", " ", null, null,
                null, null, null, HOJE, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", "Jo", null, null,
                null, null, null, HOJE, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", "a".repeat(121),
                null, null, null, null, null, HOJE, Perfil.RECEPCIONISTA));
        assertEquals("Ana", Funcionario.informado(0, "marta01", "000.000.001-91", "Ana", null, null, null, null, null,
                HOJE, Perfil.RECEPCIONISTA).nome());
        assertEquals(120, Funcionario.informado(0, "marta01", "000.000.001-91", "a".repeat(120), null, null, null, null,
                null, HOJE, Perfil.RECEPCIONISTA).nome().length());
    }

    @Test
    void estadoTelefoneEEmailSaoValidadosQuandoInformados() {
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste",
                null, null, "XX", null, null, HOJE, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste",
                null, null, null, "9999-9999", null, HOJE, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste",
                null, null, null, null, "marta.exemplo", HOJE, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste",
                null, null, null, null, "marta@exemplo", HOJE, Perfil.RECEPCIONISTA));
        Funcionario f = Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste", "Rua 1", "Dourados", "ms",
                "6730000000", "marta@exemplo.test.br", HOJE, Perfil.RECEPCIONISTA);
        assertEquals("MS", f.estado());
        assertEquals("6730000000", f.telefone());
        assertEquals("marta@exemplo.test.br", f.email());
        assertEquals("Rua 1", f.endereco());
        assertEquals("Dourados", f.cidade());
    }

    @Test
    void camposOpcionaisOmitidosOuEmBrancoFicamNulosENaoViramTextoVazio() {
        for (String ausente : new String[]{null, "", "   "}) {
            Funcionario f = Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste", ausente, ausente,
                    ausente, ausente, ausente, HOJE, Perfil.RECEPCIONISTA);
            assertNull(f.endereco());
            assertNull(f.cidade());
            assertNull(f.estado());
            assertNull(f.telefone());
            assertNull(f.email());
        }
    }

    @Test
    void dataDeContratacaoEPerfilSaoObrigatoriosEANaoPodeSerFutura() {
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste",
                null, null, null, null, null, null, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste",
                null, null, null, null, null, HOJE, null));
        Funcionario amanha = Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste", null, null, null, null,
                null, HOJE.plusDays(1), Perfil.RECEPCIONISTA);
        assertThrows(NegocioException.class, () -> amanha.exigirContratacaoAte(HOJE));
        valido().exigirContratacaoAte(HOJE);
        valido().exigirContratacaoAte(HOJE.plusDays(1));
    }

    @Test
    void oTamanhoDasColunasEhConferidoAntesDoBanco() {
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91",
                "Marta Teste", "a".repeat(201), null, null, null, null, HOJE, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91",
                "Marta Teste", null, "a".repeat(81), null, null, null, HOJE, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91",
                "Marta Teste", null, null, null, "(67) 90000-0001 r1234", null, HOJE, Perfil.RECEPCIONISTA));
        assertThrows(NegocioException.class, () -> Funcionario.informado(0, "marta01", "000.000.001-91",
                "Marta Teste", null, null, null, null, "a".repeat(108) + "@exemplo.test", HOJE, Perfil.RECEPCIONISTA));
        Funcionario limite = Funcionario.informado(0, "marta01", "000.000.001-91", "Marta Teste", "a".repeat(200),
                "a".repeat(80), null, "(67) 90000-0001 r123", "a".repeat(107) + "@exemplo.test", HOJE,
                Perfil.RECEPCIONISTA);
        assertEquals(200, limite.endereco().length());
        assertEquals(80, limite.cidade().length());
        assertEquals(20, limite.telefone().length());
        assertEquals(120, limite.email().length());
    }

    /**
     * Linhas gravadas na iteração 1, antes destas regras, não passam pelo formato atual. Reconstituí-las
     * não pode falhar, senão a listagem e o login de quem está nelas quebram.
     */
    @Test
    void aReconstituicaoDeUmaLinhaGravadaNaoAplicaAsRegrasDeFormato() {
        Funcionario antigo = new Funcionario(5, "adm_02", "00000000191", "jo", "a".repeat(300), "a".repeat(300),
                "XX", "9000-0001", "sem-arroba", HOJE, Perfil.ADMINISTRADOR);
        assertEquals("adm_02", antigo.matricula());
        assertEquals("9000-0001", antigo.telefone());
        assertEquals("sem-arroba", antigo.email());
        assertThrows(NegocioException.class, antigo::validado);

        // Mesmo reconstituindo, o que o esquema garante continua exigido.
        assertThrows(NegocioException.class, () -> new Funcionario(5, " ", "00000000191", "Marta Teste", null, null,
                null, null, null, HOJE, Perfil.ADMINISTRADOR));
        assertThrows(NegocioException.class, () -> new Funcionario(5, "adm_02", " ", "Marta Teste", null, null,
                null, null, null, HOJE, Perfil.ADMINISTRADOR));
        assertThrows(NegocioException.class, () -> new Funcionario(5, "adm_02", "00000000191", " ", null, null,
                null, null, null, HOJE, Perfil.ADMINISTRADOR));
        assertThrows(NegocioException.class, () -> new Funcionario(5, "adm_02", "00000000191", "Marta Teste", null,
                null, null, null, null, null, Perfil.ADMINISTRADOR));
        assertThrows(NegocioException.class, () -> new Funcionario(5, "adm_02", "00000000191", "Marta Teste", null,
                null, null, null, null, HOJE, null));
    }

    @Test
    void autoexclusaoEhProibidaEAExclusaoDeOutroEhPermitida() {
        Funcionario eu = valido();
        assertTrue(eu.ehOMesmoQue(funcionario(1, "outro01", "123.456.789-09", Perfil.ADMINISTRADOR)));
        assertFalse(eu.ehOMesmoQue(funcionario(2, "outro01", "123.456.789-09", Perfil.ADMINISTRADOR)));
        assertFalse(eu.ehOMesmoQue(null));
        assertThrows(NegocioException.class, () -> eu.exigirExclusaoPor(eu));
        eu.exigirExclusaoPor(funcionario(2, "outro01", "123.456.789-09", Perfil.ADMINISTRADOR));
        eu.exigirExclusaoPor(null);
    }

    @Test
    void rebaixarOProprioPerfilEhProibidoMasRebaixarOutroEPromoverSeSaoPermitidos() {
        Funcionario eu = funcionario(1, "admin01", "000.000.001-91", Perfil.RECEPCIONISTA);
        Funcionario autor = funcionario(1, "admin01", "000.000.001-91", Perfil.ADMINISTRADOR);
        assertThrows(NegocioException.class, () -> eu.exigirAlteracaoPor(autor, Perfil.ADMINISTRADOR));
        eu.exigirAlteracaoPor(autor, Perfil.RECEPCIONISTA);                      // já era recepcionista
        autor.exigirAlteracaoPor(autor, Perfil.ADMINISTRADOR);                   // segue administrador
        autor.exigirAlteracaoPor(autor, Perfil.RECEPCIONISTA);                   // promoção de si não é rebaixamento
        funcionario(2, "outro01", "123.456.789-09", Perfil.RECEPCIONISTA).exigirAlteracaoPor(autor, Perfil.ADMINISTRADOR);
        eu.exigirAlteracaoPor(null, Perfil.ADMINISTRADOR);
    }
}
