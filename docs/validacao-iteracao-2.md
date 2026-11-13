# Validação do ciclo completo e regressão da iteração 2

Issue #9. Registro do que foi executado, em que ambiente e com que resultado. Tudo o que está marcado como aprovado foi **executado** nesta rodada; o que não foi executado está dito como tal.

## Ambiente

| Item | Valor |
|---|---|
| Data | 13/11/2026 |
| Código | `main` em `e7d7288` (todas as entregas da iteração 2: #2 a #8) + os testes desta validação |
| Sistema | macOS 26.6.2, Apple M5 (arm64) |
| Java | OpenJDK 27.0.1, compilando com `--release 17` (o CI usa Temurin 17) |
| Maven | 3.10.0 |
| Banco | PostgreSQL 17.11 em container Docker descartável (`postgres:17`, mesma imagem do CI), porta 55433 |
| Interface | JavaFX com sessão gráfica do macOS; **sem** `xvfb-run`, que é como o CI roda os E2E no Linux |

```bash
docker run -d --rm --name hb-val-pg -e POSTGRES_DB=hospedabem_test -e POSTGRES_USER=hospedabem \
  -e POSTGRES_PASSWORD=hospedabem -p 55433:5432 postgres:17
export HB_TEST_DB_URL=jdbc:postgresql://localhost:55433/hospedabem_test HB_TEST_DB_USER=hospedabem HB_TEST_DB_PASSWORD=hospedabem
mvn --batch-mode checkstyle:check
mvn --batch-mode clean test -Dtest='*Test'
mvn --batch-mode test -Dtest='*IT'
mvn --batch-mode test -Dtest='*E2E' -Dhb.e2e.capturas=target/e2e-capturas
mvn --batch-mode -DskipTests package
mvn --batch-mode test-compile org.pitest:pitest-maven:mutationCoverage
```

## Resultado da suíte

| Etapa | Resultado | Tempo |
|---|---|---|
| Checkstyle | aprovado | — |
| Unitários (`*Test`) | **89** testes, 0 falhas, 0 erros | 4,8 s |
| Integração (`*IT`) | **145** testes, 0 falhas, 0 erros | 13,6 s |
| Ponta a ponta (`*E2E`) | **12** testes, 0 falhas, 0 erros | 10,6 s |
| Empacotamento (`package`) | aprovado | — |
| Mutação (PIT 1.17.4) | **541 mutantes, 517 mortos (96%)**, limiar 95% atingido | 7 min |

Total: **246 testes**. Antes desta validação o `main` (`e7d7288`) tinha **240** (89 unitários, 140 de integração, 11 E2E): a linha do UC09 na tabela de rodadas do README (236) foi registrada antes dos testes acrescentados na revisão do PR #38. Esta validação acrescenta **6** testes (5 de integração e 1 E2E), listados por critério abaixo.

## Critérios de aceitação

### 1. Ciclo operacional completo pela interface — aprovado

`TelasE2E.cicloCompletoDaIteracaoDoisPelasTelas` opera as telas reais, numa única execução e sem carga extra, na ordem pedida:

1. O administrador cadastra a funcionária `lara01` (recepcionista), a hóspede Marta (CPF 529.982.247-25) e o item AGUA (R$ 5,50).
2. `lara01` entra, troca a senha provisória (RNF-8) e fica com o menu de recepcionista.
3. Registra a reserva 1002 (STD, hoje a hoje+2) e a altera para sair em hoje+3 (UC09).
4. Faz o check-in da reserva 1002: hospedagem 5001. A disponibilidade STD de hoje cai de 2 para 1.
5. Lança 2 × AGUA (R$ 11,00) e confere a conta: R$ 191,00 (1 diária STD + consumos).
6. Tenta confirmar sem forma de pagamento, escolhe cartão de débito, **recusa** a confirmação no diálogo (nada é gravado) e depois confirma.
7. O check-out conclui, nenhum quarto fica ocupado e a disponibilidade STD volta a 2.

O teste confere no banco que a hospedagem é da `lara01`, a reserva está `CONVERTIDA` com a saída alterada, e o pagamento é `CARTAO_DEBITO` de R$ 191,00. Captura: `target/e2e-capturas/04-ciclo-completo-check-out.png`.

### 2. Cancelamento, senha vencida/troca, acesso negado e pagamento não confirmado — aprovado

| Cenário | Mensagem exibida | Teste |
|---|---|---|
| Cancelamento de reserva | "Reserva 1001 cancelada. A capacidade do tipo LUX voltou à consulta de disponibilidade." | `TelasE2E.recepcionistaAlteraECancelaReservaPelaTela` |
| Senha provisória no primeiro acesso | "Sua senha venceu ou foi definida pelo administrador. Defina uma nova senha para continuar." | `TelasE2E.administradorCadastraFuncionario...`, `cicloCompletoDaIteracaoDoisPelasTelas` |
| Senha vencida por prazo | operações recusadas até a troca | `TrocaSenhaIT.senhaVencidaBloqueiaOperacoesAteATroca`, `senhaAindaValidaUmDiaAntesDoLimite` |
| Confirmação da nova senha divergente | "A confirmação não confere com a nova senha." | `TelasE2E.administradorCadastraFuncionario...` |
| Acesso negado | menu de cadastros desabilitado para a recepcionista; chamada direta sem perfil recusada | `TelasE2E.loginRecusaSenhaErradaERecepcionistaNaoAcessaCadastros`, `AutenticacaoIT`, `*IT` de cada UC |
| Pagamento não confirmado | sem forma: "Selecione a forma de pagamento."; diálogo recusado: nada gravado | `TelasE2E.cicloCompletoDaIteracaoDoisPelasTelas`, `CheckOutIT.semConfirmacaoNadaMudaETotalDivergenteEhRecusado` |

### 3. Concorrência — aprovado

Cada teste usa duas conexões reais simultâneas contra o PostgreSQL.

| Disputa | Resultado verificado | Teste |
|---|---|---|
| Alteração × check-in da mesma reserva | 5 rodadas: exatamente uma passa; a reserva fica convertida **ou** alterada, nunca as duas | `ConcorrenciaIT.alteracaoDeReservaECheckInDaMesmaReservaNaoAcontecemAoMesmoTempo` |
| **Cancelamento × check-in da mesma reserva** (novo) | 5 rodadas: exatamente uma passa; convertida com uma hospedagem e quarto ocupado, **ou** cancelada sem hospedagem e quarto livre; uma conta por hospedagem | `ConcorrenciaIT.cancelamentoECheckInDaMesmaReservaNaoAcontecemAoMesmoTempo` |
| Alteração × redução de capacidade (UC05 de tipo) | a alteração espera a trava e valida a capacidade nova | `ConcorrenciaIT.alteracaoValidaACapacidadeGravadaPorUmUC05Concorrente` |
| Lançamento × fechamento da conta | o lançamento espera o check-out e é recusado; nenhum consumo entra depois do fechamento | `CheckOutIT.lancamentoConcorrenteComCheckOutNaoEntraDepoisDoFechamento` |
| Duas confirmações de check-out | um único pagamento registrado | `CheckOutIT.confirmacoesSimultaneasRegistramUmUnicoPagamento` |

Não houve duplicação nem perda de dados em nenhuma rodada.

### 4. Falha no fechamento e persistência após reinício — aprovado

- `CheckOutIT.falhaNaLiberacaoDoQuartoDesfazTodasAsAlteracoes` injeta uma falha por *trigger* no último passo do check-out (liberação do quarto). Depois do rollback não há pagamento, a conta continua `ABERTA` sem `fechada_em`, a hospedagem continua `ATIVA` sem saída e o quarto continua `OCUPADO`.
- **Novo:** `PersistenciaIT.checkOutSobreviveAoReinicio` conclui um check-out com consumo, sobe um **novo contexto da aplicação** (novas conexões) e confere hospedagem `ENCERRADA` com saída, conta `FECHADA`, quarto `DISPONIVEL`, pagamento em dinheiro de R$ 191,00 e consumos de R$ 11,00.
- `PersistenciaIT.dadosSobrevivemAoReinicio` (iteração 1) continua aprovado para reserva, hospedagem e conta aberta.

### 5. Atualização do banco da iteração 1 e instalação nova — aprovado

A iteração 1 não tinha migrações: o banco de uma estação real é exatamente `schema.sql` + `seed.sql` (+ `demo.sql`) entregues em `GrupoN_Iteracao1.zip`. Esses três arquivos foram copiados **sem alteração** para `src/test/resources/iteracao1/`, para que o ponto de partida não mude quando o esquema atual mudar.

**Novo:** `MigracaoIteracaoUmIT` recria esse banco, com reservas confirmadas, canceladas e convertidas, duas hospedagens ativas e contas abertas, e então:

- `atualizacaoPreservaOsDadosEDeixaOBancoIgualAoDeUmaInstalacaoNova`: a inicialização da versão nova reconhece o banco (não recria nem recarrega), mantém a contagem de linhas de todas as 8 tabelas da iteração 1 e é idempotente numa segunda estação. Depois a **estrutura** resultante (colunas, índices, restrições, sequências com valor inicial, triggers e funções do esquema `public`, comparados como texto) é idêntica à de uma instalação nova em banco vazio. Hoje os dois caminhos executam os mesmos scripts, porque o `schema.sql` atual é o da iteração 1; a comparação passa a falhar se uma mudança futura for feita só no `schema.sql` ou só numa migração.
- `operacoesDaIteracaoDoisFuncionamSobreOsDadosDaIteracaoUm`: altera a reserva 2001 e cancela a 2003 (UC09), cadastra um item (UC05), lança consumo na hospedagem 9001 aberta na iteração 1 (UC11), faz o check-out dela com o total esperado de R$ 271,00 (UC12, quarto 501 liberado) e confere o RNF-8 sobre os funcionários da iteração 1: nenhuma senha antiga vira provisória, e o vencimento segue `senha_alterada_em` — com 89 dias desde a troca o funcionário entra direto; com 90, troca a senha no próximo acesso.

Consequência prática para a implantação: numa estação instalada há mais de 90 dias, **todos** os funcionários da iteração 1 terão de trocar a senha no primeiro acesso após a atualização, porque a iteração 1 gravou `senha_alterada_em` na carga inicial e ninguém a trocou desde então. É o comportamento pedido pelo RNF-8, não um defeito, mas vale avisar a recepção antes de atualizar.

Em nenhum dos dois caminhos é preciso apagar dados.

### 6. Limites RNF-1, RNF-2 e RNF-12 — aprovado (aferição local)

**Novo:** `LimitesRnfIT` confere a configuração efetiva da conexão (`statement_timeout` 10 s e `lock_timeout` 8 s) e mede as operações da iteração 2 sobre um histórico de 20 000 reservas e 2 000 hospedagens encerradas:

| Operação | Medido | Limite |
|---|---|---|
| UC09 alterar reserva | 2 ms | 10 s (RNF-1) |
| UC09 cancelar reserva | 1 ms | 10 s (RNF-1) |
| UC11 lançar consumo | 5 ms | 10 s (RNF-1) |
| UC12 conferir conta | 2 ms | 10 s (RNF-1) |
| UC12 confirmar pagamento e check-out | 4 ms | 10 s (RNF-1) |
| UC07 disponibilidade, todos os tipos, 90 dias | 2 ms | 3 s (RNF-2) |
| UC01 login + RNF-8 troca de senha (3 hashes PBKDF2) | 82 ms | 10 s (RNF-1) |

Limitação: é uma medição local, com banco e aplicação na mesma máquina e duas conexões no máximo. **Não** houve teste de carga com 15 estações (RNF-3) nem com rede degradada.

**RNF-12 (até cinco telas no check-out):** o check-out é **uma** página (`TelaCheckOut`) com três cartões (hospedagem, conta e pagamento) mais o diálogo de confirmação. São 2 telas no total, contra o limite de 5. No ciclo completo, o usuário chega a ela por um único item do menu.

### 7. Mutação — aprovado, com as limitações já registradas

Configuração conferida no `pom.xml` e no CI (`.github/workflows/ci.yml`, job "Mutação (PIT)"): PIT 1.17.4 com o plugin JUnit 5 1.2.1, alvo `model`, `controller` e `service`, E2E excluídos, uma thread (os testes compartilham o banco de teste) e `mutationThreshold` de **95%**, que falha o build abaixo disso.

Executado nesta rodada: **541 mutantes, 517 mortos (96%)**; cobertura de linhas das classes mutadas 1010/1045 (97%). Os 24 não mortos são 17 sobreviventes e 7 sem cobertura, os mesmos grupos já descritos no README na rodada do UC09: travas que só importam com transações concorrentes nos cadastros de tipo e quarto, guardas repetidas em dois níveis, limpeza de memória da senha, o `statement_timeout` da disponibilidade, retornos ignorados de corpos de transação e `toString`/`descricao` de exibição. Os 7 sem cobertura (UC11, UC12 e RNF-8) continuam sendo lacunas reais, não equivalentes.

## Testes adicionados nesta validação

| Teste | Critério |
|---|---|
| `TelasE2E.cicloCompletoDaIteracaoDoisPelasTelas` | 1, 2 |
| `ConcorrenciaIT.cancelamentoECheckInDaMesmaReservaNaoAcontecemAoMesmoTempo` | 3 |
| `PersistenciaIT.checkOutSobreviveAoReinicio` | 4 |
| `MigracaoIteracaoUmIT` (2 testes) | 5 |
| `LimitesRnfIT.operacoesDaIteracaoDoisFicamDentroDosLimites` | 6 |

## O que não foi validado

- E2E em Linux com `xvfb-run` e em Windows (RNF-13): só no macOS nesta rodada; o CI cobre o Linux.
- 15 estações simultâneas (RNF-3) e rede degradada (RNF-1).
- Backup e restauração (RNF-4, RNF-5) e operação 24×7 (RNF-6).
