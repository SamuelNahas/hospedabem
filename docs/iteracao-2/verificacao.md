# Conferência da entrega da iteração 2 — issue #10

Execução em 13/11/2026 sobre a branch `docs/entrega-iteracao-2`, baseada em `test/validacao-iteracao-2` (`e354fcf`). A revisão e a suíte usaram uma worktree temporária, separada da árvore principal, com PostgreSQL 17 em container descartável e porta local 55440. Ambiente: macOS 26.6.2 arm64, OpenJDK 17.0.21 e Maven 3.10.0. Os resultados abaixo são desta entrega; a [rodada da #9](../validacao-iteracao-2.md) permanece histórica.

## Execução

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export HB_TEST_DB_URL=jdbc:postgresql://localhost:55440/hospedabem_test
export HB_TEST_DB_USER=hospedabem HB_TEST_DB_PASSWORD=hospedabem
mvn --batch-mode --no-transfer-progress clean test -Dhb.e2e.capturas=target/e2e-capturas
mvn --batch-mode --no-transfer-progress test-compile org.pitest:pitest-maven:mutationCoverage
docker run --rm -v "$PWD:/repo:ro" --workdir /repo rhysd/actionlint:1.7.12 -color
```

`JAVA_HOME` acima identifica o JDK usado nesta máquina; em outro ambiente, selecione o JDK 17 instalado localmente.

| Verificação | Resultado |
|---|---|
| Checkstyle | Sem violações, executado na fase `validate` |
| Suíte integrada | **247 testes: 89 unitários, 146 de integração e 12 E2E; 0 falhas, erros ou ignorados** |
| actionlint | Aprovado, imagem 1.7.12 |
| ZIP extraído em pasta limpa | 89 testes unitários aprovados e JAR gerado com o POM/configuração incluídos |
| Mermaid | Cinco diagramas passaram no parser |

A suíte completa levou 2 minutos neste ambiente; os E2E operaram janelas JavaFX reais do macOS. A carga de demonstração tem um teste novo, `DemonstracaoIT`, que verifica consumo pendente, pagamento concluído, liberação do quarto e ausência de recarga no reinício. A validação anterior tinha 246 testes.

## Revisão e sondas

Comparação: `git diff e354fcf...docs/entrega-iteracao-2`. A revisão teve dois eixos independentes: **Standards**, sem violações documentadas ou heurísticas relevantes; **Spec**, com um achado confirmado ao rodar.

Uma sonda gerou/extraiu o ZIP e conferiu links locais: o README copiado para `src/entrega/` apontava para `src/LEIA-ME.md` e `.github/workflows/ci.yml` inexistentes nessa localização; comandos também dependiam de arquivos ausentes. A correção e sua nova conferência são registradas no pacote final. Nenhuma sonda entra no código entregue.

## Capturas atuais da execução

As imagens abaixo foram geradas pelos E2E desta rodada, sem dados reais. Elas documentam execuções de testes independentes; não representam uma gravação contínua ou um vídeo da apresentação K.

| Evidência | Captura |
|---|---|
| Funcionário cadastrado | [UC02](telas/funcionario-cadastrado.png) |
| Hóspede cadastrado | [UC06](telas/hospede-incluido.png) |
| Reserva cancelada | [UC09](telas/reserva-cancelada.png) |
| Consumo lançado | [UC11](telas/consumo-lancado.png) |
| Check-out concluído | [UC12](telas/check-out-concluido.png) |
| Ciclo completo com novo funcionário/hóspede e pagamento por débito | [Ciclo integrado](telas/ciclo-completo-check-out.png) |

## Limites

Esta execução local não aprova Linux/Windows, 15 estações, rede degradada, backup/restauração ou operação 24×7. O [CI da PR #39](https://github.com/SamuelNahas/hospedabem/actions/runs/43470470582) aprovou a base `e354fcf`; isso não substitui os checks da [PR #40](https://github.com/SamuelNahas/hospedabem/pull/40).

As fontes disponíveis exigem apresentação da segunda iteração, mas não detalham o formato de envio da K. O [pacote](entrega.md) segue o precedente da entrega J; o [roteiro](demonstracao.md) permite demonstrar a aplicação. Confirmar eventuais instruções adicionais do AVA antes da submissão acadêmica.
