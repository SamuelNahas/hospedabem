# HospedaBem — Iterações 1 e 2

Sistema desktop de hospedagem em hotéis. Trabalho de Análise e Projeto de Software Orientado a Objetos (FACOM/UFMS).

**Grupo N:** Felipe de Souza Rosa, Ruan Sartório Thomaz e Samuel Vieira da Cunha Nahas.

Para colaborar no repositório, consulte o [tutorial básico de GitHub do grupo](docs/tutorial-github.md): assumir tarefas, usar o board, criar branches e abrir Pull Requests.

A iteração 1 implementou os seis casos de uso alocados na listagem de casos de uso (atividade D). A iteração 2 começa pelo UC02:

| UC | Caso de uso | Perfis | Iteração |
|----|-------------|--------|----------|
| UC01 | Autenticar-se | todos | 1 |
| UC02 | Gerenciar Funcionário | administrador | 2 |
| UC03 | Gerenciar Tipo de Quarto | administrador | 1 |
| UC04 | Gerenciar Quarto | administrador | 1 |
| UC07 | Consultar Disponibilidade de Quartos | administrador, recepcionista | 1 |
| UC08 | Registrar Reserva | administrador, recepcionista | 1 |
| UC10 | Realizar Check-in, com e sem reserva | administrador, recepcionista | 1 |

Ficam para as próximas iterações: cadastro de hóspedes (UC06), alteração e cancelamento de reservas (UC09), consumos, check-out, pagamentos, comprovantes e relatórios, e a troca periódica de senha pelo próprio funcionário (RNF-8), que parte do UC02. O menu só mostra o que foi implementado.

## Tecnologia e organização (MVC)

Java 17+, **JavaFX 21 com CSS**, Spring Boot 3.4 **sem servidor web** (`spring.main.web-application-type=none`), Spring JDBC (`JdbcTemplate`, HikariCP, `@Transactional`), PostgreSQL, Maven e JUnit 5.

### Por que cada tecnologia

Cada escolha parte de uma exigência da disciplina ou dos requisitos do HospedaBem:

| Escolha | Justificativa |
|---|---|
| **Java 17** | A definição do trabalho exige implementação em Java, desktop, com persistência em SGBD relacional (*Definição do Trabalho Prático*, p. 4). Java também mapeia diretamente o DCP: cada classe, atributo, visibilidade e método do diagrama vira um elemento da linguagem. O Java 17 é a versão de suporte longo (LTS) exigida pelo Spring Boot 3. |
| **JavaFX 21** | O trabalho é desktop. O JavaFX é o toolkit de interface atual do Java, sucessor do Swing, e roda em Windows, Linux e macOS (RNF-13). O CSS separa o visual do código das telas, de modo que a `view` só monta componentes, sem regra de negócio (*Model-View Separation*, Larman, cap. 13). |
| **Spring Boot, sem servidor web** | É usado só como contêiner de injeção de dependências e de transações; a aplicação continua desktop. A injeção cria as ligações `controller → service → DAO` pelos construtores, então nenhuma classe instancia as próprias dependências (Baixo Acoplamento, GRASP). O `@Transactional` garante que as pós-condições de um contrato valham todas ou nenhuma, e atende ao RNF-4 (dados da última operação não ficam pela metade em caso de falha). |
| **Spring JDBC (`JdbcTemplate`)** | Implementa o padrão de projeto DAO apresentado na disciplina (slides M3-U1, p. 52–53) com SQL explícito, sem framework de mapeamento objeto-relacional. Assim, o SQL de cada operação fica visível e conferível no DAO correspondente. |
| **HikariCP** | Pool de conexões com tempo limite de conexão de 10 s, exigido pelo RNF-1, e conexões reaproveitadas entre as estações da rede local (RNF-3). |
| **PostgreSQL** | É uma das duas opções do RNF-14. Foi escolhido porque o banco também protege as regras com várias recepções ao mesmo tempo (RNF-3): bloqueio de linha (`SELECT ... FOR UPDATE`), índice único parcial (uma hospedagem ativa por quarto) e `CHECK`. |
| **Maven** | Build reproduzível: as dependências e as versões ficam declaradas no `pom.xml`, e qualquer membro do grupo ou a professora compila com um comando. Também gera os componentes nativos do JavaFX para o sistema do build (RNF-13). |
| **JUnit 5 e PIT** | O trabalho é avaliado por testes funcionais caixa-preta (*Definição do Trabalho Prático*, p. 3). Os testes automatizados conferem cada regra e cada pós-condição antes disso, e o teste de mutação (PIT) mede se os testes realmente detectam erros. |

### Por que MVC em camadas, com service e DAO

| Camada | Justificativa |
|---|---|
| **Arquitetura em camadas e MVC** | O RNF-11 exige um sistema em módulos, em que partes possam ser adicionadas, excluídas ou alteradas sem afetar o todo. Cada camada só conhece a de baixo, então trocar a tela, o banco ou uma regra não afeta as outras (*Layers*, Larman, cap. 13). |
| **`view`** | Telas JavaFX sem SQL e sem regra de negócio, pelo princípio *Model-View Separation* (Larman, cap. 13). As telas chamam os controladores fora da thread do JavaFX, para a interface não travar enquanto o banco responde. |
| **`controller`** | Padrão Controlador do GRASP: recebe as operações de sistema do DSS (`iniciarCheckIn`, `adicionarHospede`, ..., `confirmarCheckIn`). Há um controlador por caso de uso (*use case controller*), e não um único para o sistema todo, para manter cada um coeso. O controlador também verifica o perfil da sessão em toda operação (RNF-10). |
| **`service`** | *Pure Fabrication* do GRASP (Larman, cap. 25): uma classe que não existe no domínio do hotel e foi criada para tirar a transação e os bloqueios das classes de domínio. |
| **`dao`** | Padrão de projeto DAO (slides M3-U1, p. 52–53): isola o acesso ao banco. Um DAO por entidade. |
| **`model`** | As regras de negócio ficam nas classes de domínio, pelo padrão Especialista na Informação: quem tem a informação tem a responsabilidade (`Quarto.exigirLivrePara`, `TipoQuarto.exigirComporta`). Por isso o modelo é testado sem banco. |

```
src/main/java/hospedabem/
├── App.java            ponto de entrada (Spring Boot + JavaFX)
├── model/              Model: entidades e TODAS as regras de negócio, sem banco nem tela (Atendimento, Quarto, Reserva, ...)
├── dao/                persistência JDBC (QuartoDAO, ReservaDAO, HospedagemDAO, ...)
├── controller/         Controller: verifica a permissão da sessão e delega ao service
├── service/            Service: um por caso de uso; transação, bloqueio e DAOs; chama as regras do model
└── view/               View: telas JavaFX (TelaLogin, TelaPrincipal, TelaCheckIn, ...), sem SQL e sem regra de negócio
src/main/resources/
├── application.properties
├── hospedabem/view/estilo.css   tema visual (CSS do JavaFX)
└── db/schema.sql, db/seed.sql
src/test/java/hospedabem/   testes unitários (*Test), de integração (*IT) e de ponta a ponta (TelasE2E)
```

As regras de negócio ficam no `model` e são testadas sem banco:

| Regra | Onde |
|---|---|
| Capacidade do tipo (reserva, check-in e alteração do tipo) | `TipoQuarto.exigirComporta` |
| Situação definível no cadastro, quarto com hospedagem ativa (RN-Q02, RN-Q04) | `Quarto.exigirSituacaoDeCadastro`, `Quarto.exigirAlteracaoPara` |
| Quarto livre para entrada (RN-CI04) | `Quarto.exigirLivrePara` |
| Reserva utilizável no check-in (RN-CI06) | `Reserva.exigirPermiteCheckIn` |
| Dados da nova reserva | `SolicitacaoReserva.validar` |
| Campos do funcionário (formato e tamanho), dígitos verificadores do CPF, autoexclusão e rebaixamento do próprio perfil | `Funcionario` |
| Composição mínima da senha | `PoliticaDeSenha` |
| Período `[entrada, saída)` e pico de ocupação | `Periodo`, `Ocupacao.pico` |
| Vaga no tipo | `Disponibilidade.exigirVaga` |
| Check-in: participantes, quarto escolhido, reserva inalterada e contrato de `confirmarCheckIn()` | `Atendimento` |

Há um service por caso de uso (`ServicoCheckIn`, `ServicoReserva`, `ServicoQuarto`, `ServicoTipoQuarto`, `ServicoDisponibilidade`, `ServicoAutenticacao`). Ele abre a transação, bloqueia o tipo, lê e grava pelos DAOs e chama essas regras. As regras ficam no model, e não no service, porque cada uma pertence ao objeto que tem a informação (Especialista na Informação), como no DCP (`Quarto.ocupar()`, `Reserva.converter()`).

Os nomes seguem o DC e o DCP: `TelaCheckIn`, `ControladorCheckIn.confirmarCheckIn()`, `QuartoDAO.buscarPorNumero/atualizar`, `HospedagemDAO.salvar`, `ReservaDAO.atualizar`, `Hospedagem.adicionarHospede/setQuarto`, `Quarto.ocupar()`, `Reserva.converter(Hospedagem)` e `ContaDaHospedagem`.

A interface é uma janela única, com barra lateral de navegação, páginas em cartões e avisos de erro e sucesso numa faixa fixa acima do conteúdo, visível mesmo com a página rolada. O tema fica em `estilo.css`. As telas chamam os controladores fora da thread do JavaFX (`view/Tarefa.java`, com `javafx.concurrent.Task`). Os controladores verificam o perfil da sessão em toda operação. Assim, uma chamada direta sem permissão também é recusada.

## Preparação

### 1. Dependências

| Ferramenta | Versão | Verificação |
|---|---|---|
| JDK | 17 ou superior | `java -version` e `javac -version` |
| Maven | 3.9 ou superior | `mvn -v` |
| PostgreSQL | 12 ou superior, ou Docker | `psql --version` ou `docker --version` |

No macOS, com Homebrew:

```bash
brew install maven openjdk@17                       # Maven 3.10 e OpenJDK 17.0.21
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
docker compose up -d                                # PostgreSQL 17 com os bancos hospedabem e hospedabem_test
```

Linux: `sudo apt install openjdk-17-jdk maven postgresql`. Windows: Temurin 17, Maven e o instalador do PostgreSQL.

### 2. Banco de dados

Com Docker, o `docker-compose.yml` cria o usuário `hospedabem`, o banco de uso `hospedabem` e o banco de testes `hospedabem_test`.

Sem Docker:

```sql
CREATE USER hospedabem WITH PASSWORD 'troque-esta-senha';
CREATE DATABASE hospedabem OWNER hospedabem;
CREATE DATABASE hospedabem_test OWNER hospedabem;
```

Na primeira execução, a aplicação cria o esquema (`src/main/resources/db/schema.sql`) e a carga fictícia (`db/seed.sql`), mas só quando o banco ainda não tem as tabelas. Ela nunca apaga dados. Para preparar o banco manualmente:

```bash
psql -U hospedabem -d hospedabem -f src/main/resources/db/schema.sql
psql -U hospedabem -d hospedabem -f src/main/resources/db/seed.sql
```

### 3. Configuração (variáveis de ambiente)

| Variável | Padrão de desenvolvimento |
|---|---|
| `HB_DB_URL` | `jdbc:postgresql://localhost:5432/hospedabem` |
| `HB_DB_USER` | `hospedabem` |
| `HB_DB_PASSWORD` | `hospedabem` |
| `HB_TEST_DB_URL`, `HB_TEST_DB_USER`, `HB_TEST_DB_PASSWORD` | o mesmo, com o banco `hospedabem_test` |

Em rede local, cada estação aponta `HB_DB_URL` para o servidor PostgreSQL, por exemplo `jdbc:postgresql://192.168.0.10:5432/hospedabem`. No servidor, libere a faixa da rede interna em `pg_hba.conf` e `listen_addresses`. Não exponha a porta à internet (RNF-9).

## Execução

```bash
mvn package -DskipTests
java -jar target/hospedabem.jar --hb.demo=true   # primeira execução, com o banco vazio: cria a carga de demonstração
java -jar target/hospedabem.jar                  # execuções seguintes
```

A opção `--hb.demo=true` só tem efeito quando o banco está vazio. Ela executa `db/demo.sql` depois de `db/seed.sql`, com mais hóspedes, o tipo EXE, os quartos 302 e 501 a 503, reservas futuras, canceladas e convertidas e duas hospedagens ativas. Os testes não usam essa carga. Para recomeçar do zero: `docker exec hospedabem-pg psql -U hospedabem -d hospedabem -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"` (apaga os dados do banco de uso).

### Credenciais de demonstração (DADOS FICTÍCIOS DE TESTE)

| Matrícula | Senha | Perfil |
|---|---|---|
| `admin01` | `Admin@123` | Administrador |
| `recep01` | `Recep@123` | Recepcionista |

Hóspedes fictícios da carga: `11111111111` (Carlos), `22222222222` (Beatriz), `33333333333` (Diego), `44444444444` (Elisa) e o passaporte `XP1234567` (John). Tipos: STD (2 pessoas, R$ 180), LUX (3, R$ 320), SUI (4, R$ 550). Quartos: 101 e 102 (STD), 103 (STD, em manutenção), 201 e 202 (LUX), 301 (SUI). A reserva **1001** (Carlos, LUX, de hoje a hoje+3, 2 hóspedes) permite demonstrar o check-in com reserva no mesmo dia da carga.

## Testes

Há quatro níveis de teste. Todos rodam com `mvn test`, exceto a mutação, que tem comando próprio.

| Nível | Arquivos | O que cobre | Banco |
|---|---|---|---|
| Unitário | `*Test` (58 testes) | Todas as regras do `model` (`Atendimento`, `Quarto`, `Reserva`, `SolicitacaoReserva`, `TipoQuarto`, `Periodo`, `Ocupacao`, `Disponibilidade`, `Funcionario`, `PoliticaDeSenha`), `Sessao`, hash de senha, tradução de duplicidade simultânea, mensagens de erro, conversão de campos | não usa |
| Integração | `*IT` (73 testes) | Controladores + services + DAOs + PostgreSQL: regras dos 7 UCs, transação do check-in, falha injetada na gravação, concorrência entre conexões, reinício | `hospedabem_test` |
| Ponta a ponta (E2E) | `TelasE2E` (7 testes) | Telas JavaFX reais operadas como um usuário (`RoboFx`): login, permissões, cadastros, funcionários, disponibilidade, reserva, check-in com e sem reserva. Confere a tela e o banco. | `hospedabem_test` |
| Mutação | PIT (`pitest-maven`) | Altera o código de `model`, `controller` e `service` e verifica se algum teste falha | `hospedabem_test` |

```bash
mvn test                      # unitários + integração + E2E (138 testes)
mvn test -Dtest='*Test'       # somente unitários, sem banco
mvn test -Dtest='TelasE2E' -Dhb.e2e.capturas=docs/telas   # E2E salvando a captura de cada passo
mvn test-compile org.pitest:pitest-maven:mutationCoverage  # mutação; relatório em target/pit-reports/index.html
```

Os testes de integração e E2E recriam o esquema **somente do banco de teste**. Antes disso, verificam se o nome do banco termina em `_test` e se recusam a rodar em outro banco. O banco de uso não é tocado.

Os E2E abrem janelas JavaFX e precisam de uma sessão gráfica: em Linux sem monitor, use `xvfb-run mvn test`. A mutação roda numa única thread, porque os testes compartilham o banco de teste.

### Resultado verificado

| Rodada | Ambiente | Testes | Mutação (PIT 1.17.4) |
|---|---|---|---|
| Iteração 1, 03/11/2026 | OpenJDK 27 com `--release 17`, PostgreSQL 17 | **102**, 0 falhas, 0 erros (41 unitários, 55 de integração, 6 E2E) | **234 mutantes, 228 mortos (97%)** |
| Iteração 2 (UC02), 05/11/2026 | OpenJDK 25 com `--release 17`, PostgreSQL 17.11 | **138**, 0 falhas, 0 erros (58 unitários, 73 de integração, 7 E2E) | **351 mutantes, 344 mortos (98%)** |

Nenhum mutante ficou sem cobertura. O build exige no mínimo 95% (`mutationThreshold`). Os 7 sobreviventes da última rodada são equivalentes ao original ou inobserváveis em teste:

- 4 remoções de `TipoQuartoDAO.bloquear` nos cadastros de tipo e quarto. O bloqueio só faz diferença com transações concorrentes. O bloqueio equivalente da reserva é verificado por `ConcorrenciaIT`, e o do UC02 (`FuncionarioDAO.bloquearAdministradores`) também, repetindo a corrida em 20 rodadas — então estes quatro são cobríveis do mesmo jeito e ficaram para uma próxima iteração, não por serem intestáveis.
- `PBEKeySpec.clearPassword()`: limpeza de memória, sem efeito observável.
- `SET LOCAL statement_timeout = 3000` da consulta de disponibilidade: só se manifesta com uma consulta lenta.
- Um `return null` do corpo da transação de `ServicoFuncionario.alterar` substituído por `null`: a lambda já devolve `null`, então o mutante é idêntico ao original.

### O que a mutação revelou

- A primeira rodada do PIT (80%) revelou lacunas reais, cobertas depois por testes: etapas do check-in fora de ordem, manutenção do quarto após o resumo com folga no tipo, reserva ou quarto apagados antes da confirmação, capacidade exatamente no limite e senha não apagada da memória. Revelou também duas pré-checagens de duplicidade redundantes com a restrição `UNIQUE` do banco, que foram removidas. Depois da separação das regras no `model`, a mutação apontou mais uma lacuna, já coberta: check-in sem reserva quando a última vaga do tipo pertence à reserva de outro hóspede. Com a criação da camada `service`, apontou `ControladorReserva.buscarResponsavel` sem teste, coberto por `ReservaIT.buscarResponsavelRetornaOHospedeCadastrado`.
- No UC02, a mutação apontou 5 lacunas reais, todas cobertas depois: os campos opcionais em branco (estado, telefone e e-mail) podiam virar texto vazio em vez de `null` sem nenhum teste reclamar; o limite exato de 120 caracteres do nome não era exercitado; e `ServicoFuncionario.alterar` conferia a data de contratação sem que nenhum teste cobrisse data futura **na alteração** — só na inclusão.
- Depois da revisão do UC02, a mutação mostrou que as duas pré-checagens de duplicidade do `ServicoFuncionario` podiam ser removidas sem nenhum teste falhar: com a tradução da violação do `UNIQUE` já implementada, a mensagem é a mesma pelos dois caminhos. As pré-checagens foram removidas, como já havia acontecido com as dos outros cadastros na iteração 1 (decisão 23).
- Os E2E encontraram um problema de interface: o item do menu e o botão da página de reserva tinham o mesmo texto. O botão passou a se chamar "Confirmar reserva". As capturas estão em `docs/telas`.

## Verificação: casos de uso × telas × testes

| UC | Tela | Controlador | Testes |
|---|---|---|---|
| UC01 | `TelaLogin`, `TelaPrincipal` (itens de cadastro desabilitados para a recepcionista) | `ControladorAutenticacao`, `Sessao` | `AutenticacaoIT` (login correto, senha incorreta, matrícula inexistente, chamadas diretas sem sessão e sem perfil, hash PBKDF2, senha apagada da memória), `SenhaTest`, `SessaoTest`, `TelasE2E` |
| UC02 | `TelaFuncionario` (lista, busca por matrícula/nome/CPF, formulário com perfil e senha inicial) | `ControladorFuncionario`, `ServicoFuncionario` | `FuncionarioTest` (campos obrigatórios, CPF com dígitos verificadores, UF, telefone, e-mail, autoexclusão, rebaixamento do próprio perfil), `PoliticaDeSenhaTest`; `ServicoFuncionarioTest` (tradução da violação do `UNIQUE` em gravações simultâneas); `FuncionarioIT` (inclusão/listagem/busca/alteração/exclusão, matrícula e CPF duplicados na inclusão e na alteração com dados preservados, acesso de recepcionista e sem sessão em todas as operações, delegação do perfil de administrador, autoexclusão, auto-rebaixamento, sessão rebaixada ou excluída por outra estação, matrícula em qualquer caixa no login, linha fora do formato antigo ainda listada e autenticando, campo maior que a coluna, exclusão com reserva e com hospedagem, somente hash gravado, login do funcionário criado, redefinição de senha, senha apagada da memória, persistência após reinício); `ConcorrenciaIT` (dois administradores se rebaixando ao mesmo tempo); `TelasE2E` (cadastro com a data já preenchida, duplicidade, delegação de perfil, recusa de senha no "Salvar", impedimento de exclusão e login do recém-cadastrado) |
| UC03 | `TelaTipoQuarto` | `ControladorTipoQuarto` | `TelasE2E`, `DominioTest`, `CadastroIT`: inclusão/busca/alteração/exclusão, código duplicado, valores inválidos, exclusão com quarto ou reserva, redução de capacidade abaixo de reserva |
| UC04 | `TelaQuarto` | `ControladorQuarto` | `TelasE2E`, `DominioTest`, `CadastroIT`: número duplicado, tipo inexistente, ocupado/reservado pelo cadastro, RN-Q04 (hospedagem ativa), RN-Q05 (manutenção/troca de tipo/exclusão inviabilizando reserva, com dados preservados), RN-Q06 (histórico) |
| UC07 | `TelaDisponibilidade` | `ControladorDisponibilidade`, `ServicoDisponibilidade` | `TelasE2E`, `OcupacaoTest` (período inválido, sobreposição, adjacência, reservas que não se sobrepõem entre si); `DisponibilidadeIT` (manutenção, convertida não contada duas vezes, cancelada, hospedagem atrasada) |
| UC08 | `TelaReserva` | `ControladorReserva` | `TelasE2E`, `ReservaIT` (válida com funcionário da sessão, capacidade, indisponibilidade, responsável/tipo/período inválidos); `ConcorrenciaIT` (última vaga) |
| UC10 | `TelaCheckIn` (4 etapas em cartões, a última com a confirmação) | `ControladorCheckIn` | `TelasE2E` (com e sem reserva, pela tela); `DominioTest`; `CheckInIT` (etapas fora de ordem, reserva ou quarto apagados, pós-condições do contrato, sem reserva, reserva inexistente/convertida/cancelada/futura, hóspede inexistente/duplicado/excesso, responsável ausente, sem quarto, reserva alterada ou cancelada após o resumo, quarto ocupado ou em manutenção após o resumo, falha na gravação sem resultado parcial, unicidade no banco); `ConcorrenciaIT` (mesmo quarto, check-in × reserva); `PersistenciaIT` (reinício) |

## Decisões de implementação e divergências dos artefatos

1. **Transação por operação de sistema.** As transações ficam nos services (`@Transactional`), chamados pelos controladores. Os DAOs usam `JdbcTemplate` e compartilham a conexão da transação corrente. Assim, as gravações separadas mostradas no DC (1.7, 1.8 e 1.9) resultam em um único commit ou rollback.
2. **Dinheiro:** `BigDecimal` e `NUMERIC(10,2)`, em vez do `double` do DCP.
3. **Datas:** `LocalDate` para datas previstas (reserva e saída prevista) e `LocalDateTime` para entrada e saída efetivas. O DCP usa `LocalDateTime` nas datas previstas da reserva, mas os requisitos e o DSS trabalham com datas.
4. **Situações e perfis** são enums, persistidos como texto com `CHECK` no banco. O enum restringe os valores no código e o `CHECK` no banco, de modo que nenhuma situação fora do modelo conceitual pode ser gravada.
5. **`confirmarCheckIn()` retorna `Hospedagem`**, como no DSS, em vez de `void` (DC/DCP). Assim, a tela mostra o código da hospedagem, a entrada, o quarto e o número da conta aberta.
6. **O controlador de check-in guarda o atendimento** (reserva, responsável, tipo, período, participantes e quarto) e implementa os eventos do DSS: `iniciarCheckIn`, `iniciarCheckInSemReserva`, `adicionarHospede`, `consultarQuartosDisponiveis`, `selecionarQuarto` e `confirmarCheckIn`. Há um objeto por tela (escopo *prototype*).
7. **Intervalo de estadia `[entrada, saída)`:** o dia da saída não consome diária. Por isso, períodos apenas adjacentes não se sobrepõem.
8. **Disponibilidade de um tipo:** `quartos operacionais − pico de ocupação`. Quartos operacionais são os do tipo que não estão em manutenção. O pico é o maior número de estadias simultâneas em algum dia do período, calculado por varredura das mudanças de ocupação (`Ocupacao.pico`). Somar todas as reservas que tocam o período não seria correto. Entram no cálculo as reservas `CONFIRMADA` e as hospedagens `ATIVA`. Não entram as reservas `CONVERTIDA`, pois a hospedagem que elas originaram já é contada, nem as `CANCELADA`.
9. **Hospedagem atrasada:** nunca é encerrada nem tem o quarto liberado automaticamente. Para consultar períodos futuros, ela ocupa até a saída prevista ou, se já atrasada, até amanhã (`GREATEST(saída prevista, hoje+1)`). O quarto físico continua indisponível para check-in até o check-out, garantido pelo índice único de hospedagem ativa por quarto.
10. **Situação "reservado":** as reservas são por tipo, sem quarto físico. Por isso, a reserva nunca altera a situação de um quarto específico. A situação reservada aparece na consulta de disponibilidade, na coluna de ocupação por tipo. O cadastro só define `DISPONIVEL` ou `MANUTENCAO`. `OCUPADO` vem do check-in. `RESERVADO` existe no domínio (RF-10), mas não é atribuído nesta iteração (RN-Q02).
11. **Manutenção** é uma situação sem período: o quarto deixa de contar para qualquer período até voltar a `DISPONIVEL`. RN-Q05 recusa a manutenção se as reservas confirmadas e as hospedagens ativas do tipo deixarem de caber.
12. **Check-in com reserva** só é aceito de hoje até a saída prevista. A hospedagem vai de hoje até a saída da reserva. Check-in antecipado exige alterar a reserva (UC09, iteração 2). **Sem reserva**, a data de entrada informada precisa ser hoje, pois o check-in registra a entrada efetiva.
13. **O responsável deve estar entre os participantes** (fluxo principal, passo 3) e cada hóspede é contado uma vez.
14. **Mudança de reserva entre o resumo e a confirmação** é detectada pela coluna `versao`, incrementada a cada atualização, e pela comparação de situação, tipo, período, quantidade e responsável.
15. **Hóspede não cadastrado:** como o UC06 é da iteração 2, a mensagem informa o impedimento. Use os hóspedes da carga inicial.
16. **Senhas:** PBKDF2-HMAC-SHA256 com 210.000 iterações e *salt* de 16 bytes. Os documentos dizem "criptografadas", mas o hash não é reversível de propósito. O login gasta o mesmo tempo para matrícula existente ou inexistente.
17. **Códigos** de reserva (a partir de 1001) e de hospedagem (a partir de 5001) vêm de sequências do banco. O código do tipo de quarto é texto (por exemplo, `STD`), e não `int` como no DCP, porque é o código que o recepcionista lê e digita nas telas de reserva e check-in.
18. **JavaFX pelo classpath:** o JAR único (Spring Boot) carrega o JavaFX do classpath. Por isso, `App` não estende `Application` e delega a `view/AplicacaoFx`. O aviso `Unsupported JavaFX configuration` no log é esperado e não afeta o funcionamento.
19. **Políticas do UC02 não especificadas nos documentos.** Os artefatos da disciplina não dizem o que fazer quando o administrador age sobre o próprio cadastro nem quando resta um único administrador. A implementação adota, e testa, três proibições:
    - **Autoexclusão:** um administrador não exclui o próprio cadastro. A sessão em uso ficaria sem dono e o acesso seria perdido por acidente. Para sair do sistema, outro administrador exclui o cadastro.
    - **Rebaixar o próprio perfil:** um administrador não troca o próprio perfil para recepcionista. A operação revogaria, no meio da sessão, a permissão que a está autorizando. Promover-se não é rebaixamento e continua permitido (é inócuo: já é administrador).
    - **Último administrador:** nem a exclusão nem o rebaixamento podem deixar o banco sem nenhum administrador, pois ninguém mais poderia gerenciar funcionários.

    As duas primeiras dependem de quem está na sessão e são verificadas no controlador, que passa o funcionário da sessão ao service (`Funcionario.exigirExclusaoPor`, `Funcionario.exigirAlteracaoPor`). A terceira **não** é uma contagem de administradores: ela decorre da reconferência do autor descrita no item 20. Como toda gravação exige que o autor ainda seja administrador no banco, e como ninguém age sobre o próprio cadastro, quem remove ou rebaixa um administrador é sempre outro administrador que acabou de ser conferido — então sobra sempre pelo menos o autor. Contar administradores seria uma segunda regra, mais fraca (sujeita a corrida) e redundante.
20. **Perfil da sessão × perfil no banco.** A `Sessao` guarda o funcionário de quando ele entrou, então `Sessao.exigir` responde pelo perfil do momento do login. Se outra estação rebaixa ou exclui esse cadastro, a sessão continuaria com os poderes antigos. Por isso o controlador passa o funcionário da sessão como **autor** e toda gravação do UC02 começa reconferindo-o no banco, dentro da transação: se o autor não existe mais ou não é mais administrador, a operação é recusada com o pedido de entrar de novo. Antes da reconferência, a transação trava as linhas dos administradores (`SELECT ... FOR UPDATE`, em ordem de `id` para não haver impasse): sem isso, dois administradores rebaixando um ao outro ao mesmo tempo leem um ao outro como administrador, gravam linhas diferentes e o hotel fica sem nenhum. `ConcorrenciaIT.doisAdministradoresNaoSeRebaixamAoMesmoTempoDeixandoOHotelSemNenhum` reproduz a corrida em 20 rodadas e falha na primeira se a trava sai.
21. **Validar o que foi digitado × reconstituir o que está gravado.** `Funcionario` tem dois caminhos de criação. `Funcionario.informado(...)` normaliza (matrícula em minúsculas, CPF formatado, UF em maiúsculas, campos em branco viram `null`) e recusa o que está fora de formato ou maior que a coluna; é o caminho da tela e do service, e nada é gravado sem passar por ele. O construtor do `record` apenas reconstitui uma linha já gravada e confere só o que o próprio esquema garante (os campos `NOT NULL`). A separação é necessária porque uma linha inserida por SQL na iteração 1 — uma matrícula `adm_02`, um telefone `9000-0001` — não passa pelo formato que o UC02 passou a exigir, e aplicar a validação na leitura derrubaria a listagem inteira e impediria o login de quem está nessa linha. Coberto por `FuncionarioIT.linhaGravadaForaDoFormatoAtualAindaEhListadaEAutentica`.
22. **Exclusão de funcionário com histórico.** Reservas e hospedagens apontam para o funcionário que as registrou com `ON DELETE RESTRICT`. O service conta os vínculos antes de apagar e recusa a exclusão com a quantidade encontrada, sugerindo alterar os dados ou o perfil. Nada do histórico é apagado nem anonimizado: quem registrou cada reserva e cada check-in continua identificável.
23. **Duplicidade de matrícula e de CPF.** O CPF é validado pelos dígitos verificadores e gravado sempre formatado (`000.000.000-00`), para que não existam duas grafias do mesmo número. Como nos outros cadastros, quem barra a repetição são as restrições `UNIQUE` do banco, que valem também entre estações simultâneas; não há pré-checagem. O que o UC02 acrescenta é a **tradução** da violação: ao receber o erro do `UNIQUE`, o service relê qual dos dois campos repetiu e devolve a mensagem que diz isso, porque a tela precisa apontar o campo a corrigir. A releitura é feita fora da transação, já que a que falhou foi abortada e não aceita mais consultas (`ServicoFuncionarioTest`). Em qualquer recusa, a tela mantém os dados digitados.
24. **Senha do funcionário.** A inclusão exige uma senha inicial e grava apenas o hash PBKDF2 (RNF-7); a senha nunca é gravada nem registrada em log, e o vetor de caracteres é zerado depois do uso. A alteração dos dados não mexe na senha: há o botão "Redefinir senha", que grava o novo hash e atualiza `senha_alterada_em`. Se houver senha digitada ao clicar em "Salvar" de um funcionário selecionado, a tela **recusa** a operação e aponta o botão certo, em vez de gravar o resto e deixar a senha antiga valendo sem avisar. O hash PBKDF2 custa 210 000 iterações e é calculado **antes** de abrir a transação, para não manter uma conexão do pool presa durante o cálculo; por isso as gravações do UC02 usam `TransactionTemplate` em vez de `@Transactional`. A composição mínima adotada é de 8 caracteres com ao menos uma letra e um número (`PoliticaDeSenha`), compartilhada com a troca pelo próprio funcionário (RNF-8, iteração 2), que usará a mesma coluna `senha_alterada_em` para detectar o vencimento.
25. **Esquema do banco no UC02.** A tabela `funcionario` já tinha, desde a iteração 1, todas as colunas do caso de uso (endereço, cidade, estado, telefone, e-mail e `senha_alterada_em`), porque a autenticação e a carga fictícia dependiam dela. Por isso o UC02 **não** alterou `schema.sql` e nenhum banco já populado precisa de atualização: `InicializadorBanco` continua criando o esquema apenas quando o banco está vazio. Nem de migração de dados, pela separação do item 21: as linhas antigas continuam sendo lidas como estão. Os limites de tamanho das colunas (endereço 200, cidade 80, telefone 20, e-mail 120) passaram a ser conferidos no `model`, para a tela dizer qual campo passou do tamanho em vez de mostrar um erro de SQL.
26. **Fonte dos diagramas:** a implementação segue as exportações `GrupoN_DC.jpg` e `GrupoN_DCP.jpg` do projeto Astah do grupo (`GrupoN.asta`).

## Transações, concorrência e falhas

- **Contrato de `confirmarCheckIn()`:** uma única transação busca e bloqueia o quarto e revalida tudo. Em seguida, cria a hospedagem com novo código, entrada atual, saída prevista e situação ativa, associa os hóspedes e o quarto, ocupa o quarto, abre exatamente uma conta e converte a reserva. Qualquer exceção faz rollback total, e o sucesso só é informado após o commit. `CheckInIT.falhaDuranteAGravacaoDesfazTudo` injeta falhas por *trigger* no meio e no último passo.
- **Concorrência entre estações:** toda operação que altera a disponibilidade de um tipo bloqueia a linha do tipo com `SELECT ... FOR UPDATE`, em ordem de id. São elas: reserva, check-in, inclusão, alteração e exclusão de quarto, e alteração de tipo. Isso serializa reservas e check-ins do mesmo tipo entre processos diferentes, e não depende de bloqueio Java. O quarto também é bloqueado na confirmação.
- **Proteções no banco:** índice único parcial de uma hospedagem `ATIVA` por quarto, `UNIQUE (hospedagem_id)` na conta, `UNIQUE (reserva_id)` na hospedagem (conversão única), chaves estrangeiras `RESTRICT` e `CHECK` de datas, valores e situações.
- **Tempos:** conexão em até 10 s (Hikari e driver), `statement_timeout` de 10 s, `lock_timeout` de 8 s (RNF-1) e consulta de disponibilidade com `statement_timeout` de 3 s (RNF-2). Ao exceder, a tela informa que não foi possível estabelecer a conexão ou concluir a operação e que nada foi gravado.
- **Recuperação (RNF-4):** a garantia oferecida é a atomicidade: uma operação confirmada (commit) fica durável no PostgreSQL (WAL), e uma operação interrompida é desfeita por completo. Basta repeti-la. **Não** há recuperação automática de uma operação perdida antes do commit, e o sistema não promete isso.
- **Backup (RNF-5):**

  ```bash
  pg_dump -U hospedabem -Fc hospedabem > hospedabem-$(date +%F).dump     # backup
  pg_restore -U hospedabem -d hospedabem --clean hospedabem-AAAA-MM-DD.dump   # restauração
  # com Docker: docker exec hospedabem-pg pg_dump -U hospedabem -Fc hospedabem > backup.dump
  ```

  Para perda mínima, configure arquivamento de WAL (`archive_mode`) no servidor. Isso depende da implantação.

## Requisitos não funcionais: como são atendidos

| RNF | Situação |
|---|---|
| RNF-1, RNF-2 | Limites configurados (10 s e 3 s). Mensagens tratadas em `view/Mensagens.java`. Não houve teste de carga nem com rede degradada. |
| RNF-3 (15 usuários) | O pool de 3 conexões por estação soma 45 conexões, abaixo do padrão de 100 do PostgreSQL. A concorrência foi testada com 2 conexões simultâneas (`ConcorrenciaIT`). **15 estações reais não foram testadas.** |
| RNF-4, RNF-5 | Atomicidade testada. Backup por `pg_dump` documentado, mas não automatizado. |
| RNF-6 (24×7) | Depende da implantação: servidor PostgreSQL com reinício automático, *nobreak* e backup. Não testado. |
| RNF-7 | Atendido com hash PBKDF2 e teste correspondente, tanto no login quanto no cadastro e na redefinição de senha do UC02. |
| RNF-8 (troca periódica) | Parcial: o UC02 já define a senha inicial, permite a redefinição pelo administrador e atualiza `senha_alterada_em` a cada troca, com a composição mínima em `PoliticaDeSenha`. Falta a troca pelo próprio funcionário e a exigência de renovação por vencimento. |
| RNF-9 | Configuração de rede local documentada. Depende do `pg_hba.conf` e do *firewall*. |
| RNF-10 | Atendido: perfis verificados no controlador e testados, inclusive a delegação do perfil de administrador no UC02, recusada para a recepcionista e para chamadas sem sessão. Nas gravações do UC02 o perfil do autor é reconferido no banco dentro da transação, para que uma sessão rebaixada por outra estação não mantenha os poderes antigos (decisão 20). |
| RNF-11 | Camadas MVC com service e DAO por entidade. Cada camada só depende da de baixo (ver "Por que MVC em camadas"). |
| RNF-12 | O check-in ocupa uma página com 4 etapas; a confirmação fica na última. Dentro do limite de 5. |
| RNF-13 | Java, JavaFX e PostgreSQL são multiplataforma. O Maven baixa os componentes nativos do JavaFX do sistema em que o build é feito: gere o JAR em cada sistema operacional de destino. **Executado apenas no macOS.** |
| RNF-14 | Atendido com PostgreSQL, uma das duas opções do requisito. O SQL usa recursos do PostgreSQL (`RETURNING`, índice parcial e `FOR UPDATE OF`) para proteger as regras no próprio banco. |

## Próximas iterações

O Processo Unificado é iterativo: cada iteração entrega um conjunto de casos de uso completo, da análise ao código testado. Ficam para as próximas iterações, conforme a listagem de casos de uso:

- UC06 (hóspedes). O modelo, as consultas e a carga fictícia que ele vai usar já existem.
- RNF-8: troca de senha pelo próprio funcionário e exigência de renovação periódica. O UC02 já entregou a definição e a redefinição da senha, a coluna `senha_alterada_em` atualizada a cada troca e a `PoliticaDeSenha`; falta definir o prazo de validade e bloquear as operações até a renovação.
- UC05, UC09, UC11 e UC12, conforme a listagem de casos de uso.

## Entrega em ZIP

`GrupoN_Iteracao1.zip` contém **somente** a pasta `src/`. Instruções para quem recebe só o ZIP estão em [src/LEIA-ME.md](src/LEIA-ME.md). Para gerar o ZIP:

```bash
rm -f GrupoN_Iteracao1.zip && zip -r -X GrupoN_Iteracao1.zip src -x '*.DS_Store' && unzip -l GrupoN_Iteracao1.zip
```
