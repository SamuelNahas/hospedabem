# HospedaBem — Iteração 1

Sistema desktop de hospedagem em hotéis. Trabalho de Análise e Projeto de Software Orientado a Objetos (FACOM/UFMS).

**Grupo N:** Felipe de Souza Rosa, Ruan Sartório Thomaz e Samuel Vieira da Cunha Nahas.

Esta iteração implementa os seis casos de uso alocados na listagem de casos de uso (atividade D):

| UC | Caso de uso | Perfis |
|----|-------------|--------|
| UC01 | Autenticar-se | todos |
| UC03 | Gerenciar Tipo de Quarto | administrador |
| UC04 | Gerenciar Quarto | administrador |
| UC07 | Consultar Disponibilidade de Quartos | administrador, recepcionista |
| UC08 | Registrar Reserva | administrador, recepcionista |
| UC10 | Realizar Check-in, com e sem reserva | administrador, recepcionista |

Ficam para as próximas iterações: cadastro de hóspedes (UC06) e de funcionários, alteração e cancelamento de reservas (UC09), consumos, check-out, pagamentos, comprovantes e relatórios. O menu só mostra o que foi implementado.

## Tecnologia e organização (MVC)

Java 17+, Swing, Spring Boot 3.4 **sem servidor web** (`spring.main.web-application-type=none`), Spring JDBC (`JdbcTemplate`, HikariCP, `@Transactional`), PostgreSQL, Maven e JUnit 5.

O Spring Boot é usado como contêiner de injeção de dependências e gerenciador de transações. A aplicação continua desktop, como exigem os documentos.

```
src/main/java/hospedabem/
├── App.java            ponto de entrada (Spring Boot + Swing)
├── model/              Model: entidades e regras (Quarto.ocupar, Reserva.converter, Hospedagem, Periodo, Ocupacao...)
├── dao/                persistência JDBC (QuartoDAO, ReservaDAO, HospedagemDAO, ...)
├── controller/         Controller: permissões, coordenação e transações (ControladorCheckIn, ...)
└── view/               View: telas Swing (TelaLogin, TelaCheckIn, ...), sem SQL e sem regra de negócio
src/main/resources/
├── application.properties
└── db/schema.sql, db/seed.sql
src/test/java/hospedabem/   testes unitários (*Test) e de integração no PostgreSQL (*IT)
```

Os nomes seguem o DC e o DCP: `TelaCheckIn`, `ControladorCheckIn.confirmarCheckIn()`, `QuartoDAO.buscarPorNumero/atualizar`, `HospedagemDAO.salvar`, `ReservaDAO.atualizar`, `Hospedagem.adicionarHospede/setQuarto`, `Quarto.ocupar()`, `Reserva.converter(Hospedagem)` e `ContaDaHospedagem`.

As telas chamam os controladores fora da thread de eventos do Swing (`view/Tarefa.java`, com `SwingWorker`). Os controladores verificam o perfil da sessão em toda operação. Assim, uma chamada direta sem permissão também é recusada.

## Preparação

### 1. Dependências

| Ferramenta | Versão | Verificação |
|---|---|---|
| JDK | 17 ou superior | `java -version` e `javac -version` |
| Maven | 3.9 ou superior | `mvn -v` |
| PostgreSQL | 12 ou superior, ou Docker | `psql --version` ou `docker --version` |

O que foi encontrado nesta máquina (macOS, 29/10/2026): o `java` do sistema não encontrava runtime, mas havia um OpenJDK 27 do Homebrew fora do `PATH`. Maven e PostgreSQL não estavam instalados. A configuração usada foi:

```bash
brew install maven                                  # Maven 3.10
export JAVA_HOME=/opt/homebrew/opt/openjdk          # JDK do Homebrew (ou: brew install openjdk@17)
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
java -jar target/hospedabem.jar
# ou: mvn spring-boot:run
```

### Credenciais de demonstração (DADOS FICTÍCIOS DE TESTE)

| Matrícula | Senha | Perfil |
|---|---|---|
| `admin01` | `Admin@123` | Administrador |
| `recep01` | `Recep@123` | Recepcionista |

Hóspedes fictícios da carga: `11111111111` (Carlos), `22222222222` (Beatriz), `33333333333` (Diego), `44444444444` (Elisa) e o passaporte `XP1234567` (John). Tipos: STD (2 pessoas, R$ 180), LUX (3, R$ 320), SUI (4, R$ 550). Quartos: 101 e 102 (STD), 103 (STD, em manutenção), 201 e 202 (LUX), 301 (SUI). A reserva **1001** (Carlos, LUX, de hoje a hoje+3, 2 hóspedes) permite demonstrar o check-in com reserva no mesmo dia da carga.

## Testes

```bash
mvn test                      # 52 testes: unitários + integração no PostgreSQL (banco hospedabem_test)
mvn test -Dtest='*Test'       # somente unitários, sem banco
```

Os testes de integração recriam o esquema **somente do banco de teste**. Antes disso, verificam se o nome do banco termina em `_test` e se recusam a rodar em outro banco. O banco de uso não é tocado.

### Resultado verificado em 29/10/2026

- `mvn test`: **52 testes, 0 falhas, 0 erros** (OpenJDK 27 com `--release 17`, PostgreSQL 17 em Docker).
- Teste de mutação: ao remover o bloqueio do tipo em `ControladorReserva`, `ConcorrenciaIT` falhou (duas reservas na última vaga). O código foi restaurado em seguida.
- Demonstração das telas reais: a aplicação foi executada com `java -jar` e criou o banco. Um roteiro em Java abriu as telas Swing reais e acionou os botões. Ele percorreu login, permissões, tipos, quartos, disponibilidade, reserva e check-in com e sem reserva. As mensagens e o estado final do banco estão em [docs/demonstracao.md](docs/demonstracao.md). Essa execução encontrou um erro real (`NullPointerException` ao abrir a tela de check-in), corrigido em `view/Tarefa.java`.

## Verificação: casos de uso × telas × testes

| UC | Tela | Controlador | Testes |
|---|---|---|---|
| UC01 | `TelaLogin`, `TelaPrincipal` (botões de admin desabilitados) | `ControladorAutenticacao`, `Sessao` | `AutenticacaoIT` (login correto, senha incorreta, matrícula inexistente, chamadas diretas sem sessão e sem perfil, hash PBKDF2), `SenhaTest` |
| UC03 | `TelaTipoQuarto` | `ControladorTipoQuarto` | `CadastroIT`: inclusão/busca/alteração/exclusão, código duplicado, valores inválidos, exclusão com quarto ou reserva, redução de capacidade abaixo de reserva |
| UC04 | `TelaQuarto` | `ControladorQuarto` | `CadastroIT`: número duplicado, tipo inexistente, ocupado/reservado pelo cadastro, RN-Q04 (hospedagem ativa), RN-Q05 (manutenção/troca de tipo/exclusão inviabilizando reserva, com dados preservados), RN-Q06 (histórico) |
| UC07 | `TelaDisponibilidade` | `ControladorDisponibilidade`, `ServicoDisponibilidade` | `OcupacaoTest` (período inválido, sobreposição, adjacência, reservas que não se sobrepõem entre si); `DisponibilidadeIT` (manutenção, convertida não contada duas vezes, cancelada, hospedagem atrasada) |
| UC08 | `TelaReserva` | `ControladorReserva` | `ReservaIT` (válida com funcionário da sessão, capacidade, indisponibilidade, responsável/tipo/período inválidos); `ConcorrenciaIT` (última vaga) |
| UC10 | `TelaCheckIn` (4 etapas + confirmação) | `ControladorCheckIn` | `CheckInIT` (pós-condições do contrato, sem reserva, reserva inexistente/convertida/cancelada/futura, hóspede inexistente/duplicado/excesso, responsável ausente, sem quarto, reserva alterada ou cancelada após o resumo, quarto ocupado ou em manutenção após o resumo, falha na gravação sem resultado parcial, unicidade no banco); `ConcorrenciaIT` (mesmo quarto, check-in × reserva); `PersistenciaIT` (reinício) |

## Decisões de implementação e divergências dos artefatos

1. **Spring Boot e MVC.** A tecnologia foi pedida pelo usuário e usada sem servidor web. As transações ficam nos controladores (`@Transactional`). Os DAOs usam `JdbcTemplate` e compartilham a conexão da transação corrente. Assim, as gravações separadas mostradas no DC (1.7, 1.8 e 1.9) resultam em um único commit ou rollback.
2. **Dinheiro:** `BigDecimal` e `NUMERIC(10,2)`, em vez do `double` do DCP.
3. **Datas:** `LocalDate` para datas previstas (reserva e saída prevista) e `LocalDateTime` para entrada e saída efetivas. O DCP usa `LocalDateTime` nas datas previstas da reserva, mas os requisitos e o DSS trabalham com datas.
4. **Situações e perfis** são enums, persistidos como texto com `CHECK` no banco.
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
17. **Códigos** de reserva (a partir de 1001) e de hospedagem (a partir de 5001) vêm de sequências do banco. O código do tipo de quarto é texto (por exemplo, `STD`), e não `int` como no DCP.
18. **Arquivo Astah (`GrupoN.asta`)** não foi aberto (formato nativo). Os diagramas foram consultados nas exportações `GrupoN_DC.jpg` e `GrupoN_DCP.jpg`.

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

## Requisitos não funcionais: situação real

| RNF | Situação |
|---|---|
| RNF-1, RNF-2 | Limites configurados (10 s e 3 s). Mensagens tratadas em `view/Mensagens.java`. Não houve teste de carga nem com rede degradada. |
| RNF-3 (15 usuários) | O pool de 3 conexões por estação soma 45 conexões, abaixo do padrão de 100 do PostgreSQL. A concorrência foi testada com 2 conexões simultâneas (`ConcorrenciaIT`). **15 estações reais não foram testadas.** |
| RNF-4, RNF-5 | Atomicidade testada. Backup por `pg_dump` documentado, mas não automatizado. |
| RNF-6 (24×7) | Depende da implantação: servidor PostgreSQL com reinício automático, *nobreak* e backup. Não testado. |
| RNF-7 | Atendido com hash PBKDF2 e teste correspondente. |
| RNF-8 (troca periódica) | **Adiado.** A coluna `senha_alterada_em` existe, mas a troca e a expiração de senha dependem do gerenciamento de funcionários (iteração 2). Não há tela de troca de senha nesta entrega. |
| RNF-9 | Configuração de rede local documentada. Depende do `pg_hba.conf` e do *firewall*. |
| RNF-10 | Atendido: perfis verificados no controlador e testados. |
| RNF-11 | Camadas MVC e DAOs por entidade. |
| RNF-12 | O check-in ocupa 1 janela com 4 etapas e a confirmação, dentro do limite de 5. |
| RNF-13 | Java/Swing e PostgreSQL são multiplataforma. **Executado apenas no macOS.** |
| RNF-14 | PostgreSQL. MySQL não foi implementado: o SQL usa `RETURNING`, índice parcial e `FOR UPDATE OF`, específicos do PostgreSQL. |

## Pendências reais

- UC06 (hóspedes) e o gerenciamento de funcionários: nesta iteração existem apenas o modelo, as consultas e a carga fictícia.
- RNF-8: troca periódica de senha.
- Execução validada em JDK 27 com `--release 17`. Não houve execução em um JDK 17 real.
- Vídeo da entrega: precisa ser gravado com a execução real. Siga [docs/roteiro-demonstracao.md](docs/roteiro-demonstracao.md).

## Entrega em ZIP

`GrupoN_Iteracao1.zip` contém **somente** a pasta `src/`. Instruções para quem recebe só o ZIP estão em [src/LEIA-ME.md](src/LEIA-ME.md). Para gerar o ZIP:

```bash
rm -f GrupoN_Iteracao1.zip && zip -r -X GrupoN_Iteracao1.zip src -x '*.DS_Store' && unzip -l GrupoN_Iteracao1.zip
```
