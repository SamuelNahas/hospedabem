# Pacote da iteração 2

## Formato e fontes

O enunciado `Definição do Trabalho Prático.pdf`, na seção K, prevê a apresentação da segunda iteração; `Etapas finais do Trabalho Prático.pdf` descreve a implementação desktop em camadas com persistência relacional. Nos arquivos consultados não há estrutura de ZIP nem formato/duração de vídeo obrigatórios para K. O pacote segue o **precedente da entrega J da equipe**, `GrupoN_Iteracao1.zip`: somente `src/` no primeiro nível. Essa escolha de empacotamento é da equipe, não uma exigência documental comprovada. Consulte [artefatos e fontes](artefatos.md) e o [roteiro](demonstracao.md).

## Gerar e reconstruir

Na raiz do repositório:

```sh
sh scripts/empacotar-iteracao-2.sh
```

O resultado é `target/GrupoN_Iteracao2.zip`. O script confere a integridade do arquivo e recusa entradas fora de `src/`. Ele copia a pasta fonte, acrescentando `src/entrega/pom.xml`, `src/entrega/config/checkstyle.xml`, o README e a documentação atuais. A cópia ocorre no staging temporário; não cria versões duplicadas desses arquivos na árvore fonte. Rode o script novamente depois de qualquer alteração para que o ZIP contenha a entrega final.

O README empacotado é uma referência do repositório completo; seus comandos de Docker, CI e geração do ZIP pressupõem esse checkout. O aviso inicial encaminha a quem recebeu somente o ZIP para `src/LEIA-ME.md`, com os comandos próprios de reconstrução e execução. Os links da documentação para arquivos de CI/templates ausentes no pacote apontam para o GitHub.

Para conferir usando apenas o ZIP, extraia-o em uma pasta vazia. Dentro dessa pasta:

```sh
cp src/entrega/pom.xml pom.xml
cp -R src/entrega/config config
mvn --batch-mode --no-transfer-progress test -Dtest='*Test'
mvn --batch-mode --no-transfer-progress -DskipTests package
```

Os comandos geram `target/hospedabem.jar`; veja `src/LEIA-ME.md` para bancos, credenciais e execução. JavaFX usa componentes nativos da plataforma do build: gere o JAR no sistema de destino. Não confunda o ZIP de fontes com um instalador ou JAR universal.

Para executar os testes de integração e E2E do pacote, configure exclusivamente um banco `*_test` descartável, separado do banco de demonstração ou de uso. Os testes recriam o esquema. Depois rode `mvn test -Dtest='*IT'` e `mvn test -Dtest='*E2E'`; E2E exige sessão gráfica, ou `xvfb-run --auto-servernum` no Linux sem monitor.

## Conferência executada nesta implementação

Em 13/11/2026, no macOS com OpenJDK 27 compilando para Java 17 e Maven 3.10:

- `DemonstracaoIT`: inicialização completa em PostgreSQL 17 descartável, 1 teste aprovado sem falhas/erros; Checkstyle sem violações.
- Regressão focada `mvn test -Dtest='DemonstracaoIT,ItemConsumoIT,MigracaoIteracaoUmIT'`: 12 testes aprovados, sem falhas/erros, incluindo inicialização sem demonstração e atualização dos dados da primeira iteração.
- O ZIP foi gerado, verificado com `unzip -tq`, extraído em pasta limpa e reconstruído a partir do POM e da configuração incluídos: 89 testes unitários aprovados sem falhas/erros e JAR empacotado com sucesso.

Na [conferência integrada](verificacao.md), a suíte completa passou com Java 17: 247 testes, sem falhas ou erros, com capturas reais da interface. A validação anterior de 246 testes e PIT de 96% está no relatório da #9 e não significa nova execução de mutação nesta atividade; o resultado da nova rodada de PIT deve ser consultado na conferência integrada.

Não foram demonstrados Windows, 15 estações simultâneas, rede degradada, backup/restauração ou operação 24×7 nesta atividade. UC13–UC18, incluindo comprovantes e relatórios, continuam identificados como iteração 3.
