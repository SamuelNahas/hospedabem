package hospedabem.controller;

import hospedabem.BancoDeTeste;
import hospedabem.model.FormaPagamento;
import hospedabem.model.ItemConsumo;
import hospedabem.model.PoliticaDeSenha;
import hospedabem.model.SituacaoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Banco de uma estação da iteração 1, recriado com os scripts entregues em
 * {@code GrupoN_Iteracao1.zip} (copiados para {@code src/test/resources/iteracao1}), inclusive a
 * carga de demonstração com reservas, hospedagens ativas e contas abertas. A iteração 1 não tinha
 * migrações: esse é exatamente o banco que uma estação real teria ao receber a versão nova.
 */
class MigracaoIteracaoUmIT extends IntegracaoBase {
    static final List<String> TABELAS_DA_ITERACAO_UM = List.of("funcionario", "hospede", "tipo_quarto", "quarto",
            "reserva", "hospedagem", "hospedagem_hospede", "conta_hospedagem");

    @Autowired ControladorItemConsumo itens;
    @Autowired ControladorConsumo consumos;
    @Autowired ControladorCheckOut checkOuts;

    @BeforeEach
    void bancoDaIteracaoUm() {
        BancoDeTeste.limpar(jdbc);
        for (String script : List.of("schema", "seed", "demo")) inicializador.executar("iteracao1/" + script + ".sql");
    }

    @Test
    void atualizacaoPreservaOsDadosEDeixaOBancoIgualAoDeUmaInstalacaoNova() {
        Map<String, Integer> antes = linhasPorTabela();

        assertFalse(inicializador.inicializarSeVazio()); // banco com dados: não recria nem recarrega
        assertEquals(antes, linhasPorTabela());
        assertFalse(inicializador.inicializarSeVazio()); // segunda estação: nada muda
        assertEquals(antes, linhasPorTabela());

        String atualizado = estrutura();
        BancoDeTeste.recriar(jdbc, inicializador);
        assertEquals(estrutura(), atualizado, "instalação nova (esperado) × banco da iteração 1 atualizado");
    }

    /** As operações da iteração 2 funcionam sobre linhas gravadas pela iteração 1. */
    @Test
    void operacoesDaIteracaoDoisFuncionamSobreOsDadosDaIteracaoUm() {
        inicializador.inicializarSeVazio();
        logarComo("recep01");

        // UC09 sobre reservas da carga antiga.
        assertEquals(HOJE.plusDays(5), reservas.alterar(reservas.consultar(2001), "88888888888", "EXE",
                HOJE.plusDays(1), HOJE.plusDays(5), 2).getPeriodo().saida());
        assertEquals(SituacaoReserva.CANCELADA, reservas.cancelar(reservas.consultar(2003)).getSituacao());

        // UC05, UC11 e UC12 sobre a hospedagem 9001, aberta na iteração 1 (quarto 501).
        logarComo("admin01");
        itens.incluir(new ItemConsumo(0, "AGUA", "Água mineral", "Frigobar", new BigDecimal("5.50")));
        logarComo("recep01");
        consumos.lancar(9001, "AGUA", 2);
        BigDecimal total = checkOuts.conferir(9001).total();
        assertEquals(new BigDecimal("271.00"), total); // entrou ontem: 1 diária do EXE (260,00) + 2 × 5,50
        checkOuts.confirmarPagamento(9001, FormaPagamento.PIX, total);
        assertEquals("DISPONIVEL", situacaoQuarto(501));
        assertEquals("ENCERRADA", jdbc.queryForObject("SELECT situacao FROM hospedagem WHERE codigo = 9001", String.class));

        // RNF-8 sobre funcionários da iteração 1: nenhuma senha antiga vira provisória; o vencimento
        // segue a data da última troca, então quem trocou há menos de 90 dias entra direto e quem
        // passou do prazo troca a senha no próximo acesso, como qualquer funcionário.
        assertEquals(0, contar("SELECT COUNT(*) FROM funcionario WHERE senha_provisoria"));
        jdbc.update("UPDATE funcionario SET senha_alterada_em = now() - make_interval(days => ?) WHERE matricula = 'recep01'",
                PoliticaDeSenha.VALIDADE_EM_DIAS - 1);
        jdbc.update("UPDATE funcionario SET senha_alterada_em = now() - make_interval(days => ?) WHERE matricula = 'admin01'",
                PoliticaDeSenha.VALIDADE_EM_DIAS);
        sessao.encerrar();
        autenticacao.autenticar("recep01", "Recep@123".toCharArray());
        assertFalse(autenticacao.senhaVencida());
        autenticacao.autenticar("admin01", "Admin@123".toCharArray());
        assertTrue(autenticacao.senhaVencida());
    }

    private Map<String, Integer> linhasPorTabela() {
        Map<String, Integer> linhas = new TreeMap<>();
        for (String t : TABELAS_DA_ITERACAO_UM) linhas.put(t, contar("SELECT COUNT(*) FROM " + t));
        return linhas;
    }

    /** Colunas, índices, restrições, sequências, triggers e funções do esquema public, em texto ordenado. */
    private String estrutura() {
        return String.join("\n", jdbc.queryForList("""
                SELECT 'coluna ' || table_name || '.' || column_name || ' ' || data_type || ' '
                       || is_nullable || ' ' || COALESCE(column_default, '') || ' ' || COALESCE(generation_expression, '')
                FROM information_schema.columns WHERE table_schema = 'public'
                UNION ALL SELECT 'indice ' || indexdef FROM pg_indexes WHERE schemaname = 'public'
                UNION ALL SELECT 'restricao ' || conrelid::regclass || ' ' || conname || ' ' || pg_get_constraintdef(oid)
                FROM pg_constraint WHERE connamespace = 'public'::regnamespace
                UNION ALL SELECT 'sequencia ' || sequencename || ' ' || start_value || ' ' || increment_by
                FROM pg_sequences WHERE schemaname = 'public'
                UNION ALL SELECT 'trigger ' || tgrelid::regclass || ' ' || pg_get_triggerdef(oid)
                FROM pg_trigger WHERE NOT tgisinternal
                UNION ALL SELECT 'funcao ' || p.oid::regprocedure || ' ' || md5(p.prosrc)
                FROM pg_proc p WHERE p.pronamespace = 'public'::regnamespace
                ORDER BY 1""", String.class));
    }
}
