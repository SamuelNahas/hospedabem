package hospedabem.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC06: regras de formato do hóspede, exercitadas sem banco. */
class HospedeTest {
    private static final LocalDate NASCIMENTO = LocalDate.of(1990, 5, 20);
    private static final LocalDate HOJE = LocalDate.of(2025, 11, 9);

    private static Hospede informado(String documento) {
        return Hospede.informado(0, documento, "Marta Teste", NASCIMENTO, null, null, null, "(67) 90000-0001", null);
    }

    @Test
    void cpfEhAceitoComOuSemPontuacaoEGuardadoSoComDigitos() {
        assertEquals("52998224725", informado("529.982.247-25").cpfOuPassaporte());
        assertEquals("52998224725", informado(" 52998224725 ").cpfOuPassaporte());
        assertTrue(informado("529.982.247-25").ehCpf());
    }

    @Test
    void passaporteDeEstrangeiroEhAceitoENormalizadoEmMaiusculas() {
        Hospede estrangeiro = informado(" xp 123/4567 ");
        assertEquals("XP1234567", estrangeiro.cpfOuPassaporte());
        assertFalse(estrangeiro.ehCpf());
    }

    @Test
    void identificacaoInvalidaOuAusenteEhRecusada() {
        // Onze dígitos é tratado como CPF, então um número fora dos dígitos verificadores não passa.
        // Letras de outros alfabetos (Х e Р cirílicos) e acentos driblariam a restrição UNIQUE.
        for (String invalido : new String[]{null, "", "   ", "529.982.247-26", "11111111111", "1234",
                "XP12", "XP#1234567", "A".repeat(21), "1".repeat(21), "ХР1234567", "ÉÇÃÕ1"}) {
            assertThrows(NegocioException.class, () -> informado(invalido), "aceitou " + invalido);
        }
    }

    @Test
    void passaporteSoNumericoEhAceito() {
        Hospede britanico = informado("925665416");
        assertEquals("925665416", britanico.cpfOuPassaporte());
        assertFalse(britanico.ehCpf());
    }

    @Test
    void telefoneInternacionalEhAceitoComMais() {
        assertEquals("+1 555 0100", Hospede.informado(0, "XP1234567", "John Test", NASCIMENTO, null, null, null,
                "+1 555 0100", null).telefone());
        assertEquals("+123456789012345", Hospede.informado(0, "XP1234567", "John Test", NASCIMENTO, null, null,
                null, "+123456789012345", null).telefone());
        assertEquals("(67) 3000-0001 r12", Hospede.informado(0, "XP1234567", "John Test", NASCIMENTO, null,
                null, null, "(67) 3000-0001 r12", null).telefone());
        for (String telefone : new String[]{"+1 555 010", "+1234567890123456", "555 0100", "abc11987654321",
                "(67) 9000+0-0001", "67 90000-0001 r"}) {
            assertThrows(NegocioException.class, () -> Hospede.informado(0, "XP1234567", "John Test", NASCIMENTO,
                    null, null, null, telefone, null), "aceitou " + telefone);
        }
    }

    @Test
    void nomeTelefoneEDataDeNascimentoSaoObrigatorios() {
        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "  ", NASCIMENTO,
                null, null, null, "(67) 90000-0001", null));
        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "Ab", NASCIMENTO,
                null, null, null, "(67) 90000-0001", null));
        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "Marta Teste", null,
                null, null, null, "(67) 90000-0001", null));
        for (String telefone : new String[]{null, "", "  ", "999"}) {
            assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "Marta Teste",
                    NASCIMENTO, null, null, null, telefone, null));
        }
    }

    @Test
    void camposOpcionaisAusentesViramNuloEOsInvalidosSaoRecusados() {
        Hospede minimo = Hospede.informado(0, "52998224725", "  Marta   Teste  ", NASCIMENTO, "  ", "", null,
                "(67) 90000-0001", "   ");
        assertNull(minimo.endereco());
        assertNull(minimo.cidade());
        assertNull(minimo.estado());
        assertNull(minimo.email());
        assertEquals("Marta Teste", minimo.nome());

        Hospede completo = Hospede.informado(0, "52998224725", "Marta Teste", NASCIMENTO, "Rua Fictícia, 1",
                "Campo Grande", "ms", "(67) 90000-0001", "marta@exemplo.test");
        assertEquals("MS", completo.estado());
        assertEquals("marta@exemplo.test", completo.email());

        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "Marta Teste", NASCIMENTO,
                null, null, "XX", "(67) 90000-0001", null));
        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "Marta Teste", NASCIMENTO,
                null, null, null, "(67) 90000-0001", "marta.exemplo.test"));
        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "X".repeat(121), NASCIMENTO,
                null, null, null, "(67) 90000-0001", null));
        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "Marta Teste", NASCIMENTO,
                "R".repeat(201), null, null, "(67) 90000-0001", null));
    }

    @Test
    void dataDeNascimentoFuturaOuAbsurdaEhRecusada() {
        informado("52998224725").exigirNascimentoAte(HOJE);
        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "Marta Teste",
                HOJE.plusDays(1), null, null, null, "(67) 90000-0001", null).exigirNascimentoAte(HOJE));
        assertThrows(NegocioException.class, () -> Hospede.informado(0, "52998224725", "Marta Teste",
                HOJE.minusYears(131), null, null, null, "(67) 90000-0001", null).exigirNascimentoAte(HOJE));
    }

    /**
     * O construtor só reconstitui a linha gravada: os documentos fictícios da carga inicial da
     * iteração 1 não passam pelos dígitos verificadores e não podem derrubar a listagem.
     */
    @Test
    void linhaGravadaDaCargaInicialEhReconstituidaSemAsValidacoesDeFormato() {
        Hospede carga = new Hospede(1, "11111111111", "Carlos Teste Silva", NASCIMENTO, null, null, null,
                "(67) 91111-1111", null);
        assertEquals("11111111111", carga.cpfOuPassaporte());
        assertThrows(NegocioException.class, carga::validado);
        assertThrows(NegocioException.class, () -> new Hospede(1, null, "Carlos", NASCIMENTO, null, null, null,
                "(67) 91111-1111", null));
        assertThrows(NegocioException.class, () -> new Hospede(1, "11111111111", "Carlos", NASCIMENTO, null, null,
                null, null, null));
        // Em branco é aceito pelo esquema; a linha é lida e só a regravação exige corrigir.
        Hospede emBranco = new Hospede(1, "11111111111", " ", NASCIMENTO, null, null, null, "", null);
        assertEquals("", emBranco.telefone());
    }

    /**
     * Limites exatos aceitos: o tamanho de cada campo é conferido antes do banco, e um limite
     * deslocado em um caractere passaria a recusar cadastro válido ou a deixar erro de SQL aparecer.
     */
    @Test
    void limitesExatosDeTamanhoEDeTelefoneSaoAceitos() {
        assertEquals("Ana", Hospede.informado(0, "52998224725", "Ana", NASCIMENTO, null, null, null,
                "(67) 90000-0001", null).nome());
        assertEquals(120, Hospede.informado(0, "52998224725", "N".repeat(120), NASCIMENTO, null, null, null,
                "(67) 90000-0001", null).nome().length());

        // Passaporte nos dois extremos do intervalo de 5 a 20.
        assertEquals("AB123", informado("AB123").cpfOuPassaporte());
        assertEquals("AB" + "1".repeat(18), informado("AB" + "1".repeat(18)).cpfOuPassaporte());

        // Telefone com o mínimo de 10 dígitos (DDD e número) e com os 20 caracteres da coluna.
        assertEquals("(67) 3000-0001", Hospede.informado(0, "52998224725", "Marta Teste", NASCIMENTO, null, null,
                null, "(67) 3000-0001", null).telefone());
        assertEquals(20, Hospede.informado(0, "52998224725", "Marta Teste", NASCIMENTO, null, null, null,
                "(67) 90000-0001 r123", null).telefone().length());

        Hospede nosLimites = Hospede.informado(0, "52998224725", "Marta Teste", NASCIMENTO, "E".repeat(200),
                "C".repeat(80), "MS", "(67) 90000-0001", "a".repeat(113) + "@b.test");
        assertEquals(200, nosLimites.endereco().length());
        assertEquals(80, nosLimites.cidade().length());
        assertEquals(120, nosLimites.email().length());
    }

    @Test
    void comIdPreservaOsDemaisCamposDoCadastro() {
        Hospede sem = informado("52998224725");
        Hospede com = sem.comId(7);
        assertEquals(7, com.id());
        assertEquals(sem.cpfOuPassaporte(), com.cpfOuPassaporte());
        assertEquals(sem.nome(), com.nome());
        assertEquals("52998224725 - Marta Teste", com.toString());
    }
}
