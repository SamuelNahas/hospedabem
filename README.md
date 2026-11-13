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
| UC06 | Gerenciar Hóspede | administrador, recepcionista | 2 |
| UC07 | Consultar Disponibilidade de Quartos | administrador, recepcionista | 1 |
| UC08 | Registrar Reserva | administrador, recepcionista | 1 |
| UC09 | Consultar, Alterar e Cancelar Reserva | administrador, recepcionista | 2 |
| UC10 | Realizar Check-in, com e sem reserva | administrador, recepcionista | 1 |

Na iteração 2 também foram entregues o cadastro de hóspedes (UC06), a alteração e o cancelamento de reservas (UC09), o lançamento de consumo (UC11), o check-out com pagamento (UC12) e a troca periódica de senha (RNF-8). Ficam para as próximas iterações: comprovantes/extrato impresso (UC14) e relatórios. O menu só mostra o que foi implementado.

## Iteração 2 — UC09: consultar, alterar e cancelar reserva

A recepção e o administrador acessam **Atendimento → Alterar/cancelar reserva**, localizam a reserva pelo **código** e veem os dados e a **situação** antes de qualquer ação — é a consulta exigida pelo caso de uso, e é também o que a tela devolve ao controlador na gravação. Dali é possível alterar responsável, tipo de quarto, datas previstas e quantidade de hóspedes, ou cancelar a reserva mediante confirmação na interface. As duas ações são de atendimento e valem para os **dois perfis** autenticados, como a reserva (UC08) e o check-in (UC10); chamadas sem sessão são recusadas no `ControladorReserva` (RNF-10).

A alteração reaproveita as regras do UC08: o responsável precisa estar cadastrado (UC06), o tipo precisa existir, o período é `[entrada, saída)` com saída posterior à entrada (`Periodo`), a quantidade precisa ser positiva e caber na capacidade do tipo (`TipoQuarto.exigirComporta`) e precisa haver vaga no período (`Disponibilidade.exigirVaga`). O que o UC09 acrescenta:

- **A própria vaga continua sendo da reserva.** O recálculo usa `ServicoDisponibilidade.calcular(tipo, periodo, reservaIgnorada)`, a mesma sobrecarga que o check-in usa, com a reserva alterada como ignorada. Sem isso, prorrogar a última reserva de um tipo com um único quarto seria recusada por ela mesma. Coberto por `ReservaIT.alteracaoMantemAPropriaVagaDoTipoComUmSoQuarto`. Ignorar a própria reserva **não** dispensa a vaga no período novo: prorrogar sobre o período de outra reserva continua sendo recusado.
- **Mudança de tipo envolve dois tipos.** A origem devolve capacidade e o destino a consome, e os dois entram no **mesmo** `TipoQuartoDAO.bloquear`, que ordena os ids — então duas alterações que trocam tipos em sentidos opostos não se esperam uma à outra. O destino é validado pela capacidade e pela disponibilidade; a origem, por ser a que está gravada (ver o item seguinte).
- **Entrada que já chegou.** A entrada só é conferida contra hoje **quando muda**: a reserva cuja entrada é hoje ou já passou continua alterável nos outros campos, mas não pode ser remarcada para trás. Por isso `SolicitacaoReserva.validar` foi separada em `validarDados()` (quantidade e capacidade, comum ao UC08 e ao UC09) e `exigirEntradaNaoPassada(hoje)`.
- **Alteração sem nenhuma mudança é recusada.** Gravar os mesmos dados subiria a `versao` e derrubaria, sem motivo, um check-in que já tinha passado pelo resumo. A mensagem pede para alterar algum campo ou cancelar.

Cancelamento: a situação vira `CANCELADA` e **nada é apagado** — a linha continua com responsável, tipo, período e funcionário que a registrou, e a capacidade volta à consulta de disponibilidade (UC07), porque `DisponibilidadeDAO` só conta reservas `CONFIRMADA`. Diferente da alteração, o cancelamento **não** bloqueia a linha do tipo de quarto: ele apenas devolve capacidade, e nenhuma operação simultânea fica com a conta errada por ainda ver a vaga ocupada — travar a reserva basta para não desfazer uma conversão em curso.

**Situações recusadas, e a decisão sobre a reserva já cancelada.** A reserva `CONVERTIDA` já virou hospedagem e não volta atrás: nem alteração, nem cancelamento. Para a reserva **já `CANCELADA`** os artefatos não dizem o que fazer, e a equipe decidiu **recusar** as duas ações, em vez de tratar o cancelamento como idempotente: a tela mostra a situação antes de agir, então repetir a ação significa que a tela está desatualizada, e uma recusa que nomeia a situação (`Reserva 1001 está cancelada e não pode mais ser alterada nem cancelada.`) informa isso, enquanto um "cancelada com sucesso" silencioso esconderia o engano. Quem quiser a estadia de volta registra uma reserva nova, que passa pela disponibilidade do momento — reabrir a cancelada burlaria essa verificação. A recusa **não** incrementa a `versao`. A regra fica em `Reserva.exigirAlteravel`, usada pelos dois caminhos.

**Versão, perda de atualização e conflito com o check-in.** Toda modificação incrementa `reserva.versao` (`ReservaDAO.alterar` e `ReservaDAO.atualizar`, ambos com `versao = versao + 1`), dentro da transação do service. Antes de gravar, o service trava a linha (`buscarPorCodigoParaAtualizar`) e compara o estado gravado com a reserva que a tela localizou, por `Reserva.mesmoEstadoDe` — o mesmo método que o check-in usa em `Atendimento.exigirReservaInalterada`. Se outra estação alterou, cancelou ou converteu a reserva nesse meio-tempo, a operação é recusada com o pedido de localizá-la de novo, e nada é gravado. Do outro lado, o UC09 é justamente o que produz a alteração que o UC10 detecta: os testes do check-in que antes **simulavam** alteração e cancelamento com `UPDATE` direto agora chamam `reservas.alterar(...)` e `reservas.cancelar(...)` pelo fluxo real da aplicação (`CheckInIT.reservaAlteradaAposOResumoImpedeConfirmacao`, `reservaCanceladaAposOResumoImpedeConfirmacao` e `reservaInexistenteConvertidaOuCanceladaNaoIniciaCheckIn`).

**Ordem dos bloqueios.** A alteração bloqueia na mesma ordem que `ServicoCheckIn.confirmar`: **tipos de quarto primeiro**, em ordem crescente de id, **depois a reserva**, **depois o hóspede responsável**. Como as duas operações disputam exatamente os mesmos recursos e os pedem na mesma ordem, nunca se esperam em sentidos opostos — não há impasse, mesmo quando a alteração envolve dois tipos. `ConcorrenciaIT.alteracaoDeReservaECheckInDaMesmaReservaNaoAcontecemAoMesmoTempo` repete a corrida em 5 rodadas e exige que exatamente uma das duas passe, e `ConcorrenciaIT.duasAlteracoesNaoRemarcamParaAUltimaVagaDoTipo` mostra que, sem o bloqueio do tipo de destino, duas remarcações simultâneas caberiam no mesmo quarto.

**Banco.** A tabela `reserva` já tinha, desde a iteração 1, todas as colunas que o UC09 grava, inclusive `versao`, então nenhum banco populado precisa de conversão de dados. A migração `db/migracoes/007_reservas.sql` acrescenta apenas o índice `reserva (tipo_quarto_id, situacao)`, que é o filtro da consulta de disponibilidade — agora executada a cada alteração. É idempotente (`CREATE INDEX IF NOT EXISTS`), não apaga nem recarrega dados, e o `InicializadorBanco` só a executa enquanto o índice não existir.

O UC09 segue as camadas dos demais casos de uso, reaproveitando as classes do UC08: as regras em `Reserva` (`exigirAlteravel`, `cancelar`, `alteradaPara`) e `SolicitacaoReserva`, a gravação em `ReservaDAO.alterar`, a transação e os bloqueios em `ServicoReserva` (`consultar`, `alterar`, `cancelar`), a autorização em `ControladorReserva` e a tela em `TelaAlteracaoReserva`. Os testes focados podem ser executados com `mvn test -Dtest='DominioTest,ReservaIT'`, `mvn test -Dtest=ConcorrenciaIT` e `mvn test -Dtest='TelasE2E#recepcionistaAlteraECancelaReservaPelaTela'`; todos usam exclusivamente `hospedabem_test`.

## Iteração 2 — UC06: cadastro de hóspedes

A recepção e o administrador acessam **Cadastros → Hóspedes** para incluir, listar, buscar e alterar hóspedes e excluir os que ainda não têm vínculo. Diferente dos cadastros do UC02, UC03, UC04 e UC05, este **não** é restrito ao administrador: regularizar o cadastro é parte do atendimento, e é a recepcionista que o faz no meio de uma reserva ou de um check-in. Chamadas sem sessão continuam recusadas no controlador (RNF-10).

Obrigatórios: identificação (CPF ou passaporte), nome, data de nascimento e telefone — o telefone porque a coluna sempre foi `NOT NULL`. O telefone nacional precisa de DDD (ao menos 10 dígitos); o internacional começa com `+` e tem de 8 a 15 dígitos, como o `+1 555 0100` do John. Opcionais: endereço, cidade, estado e e-mail; em branco, são gravados como `NULL`.

Decisão da equipe para os RF-5, RF-7 e RF-8: a identificação é **CPF quando tem exatamente 11 dígitos** e **passaporte** caso contrário. O CPF é validado pelos dígitos verificadores e guardado apenas com dígitos; o passaporte aceita de 5 a 20 letras de A a Z e algarismos (há passaportes só numéricos, como os britânicos) e é guardado em maiúsculas; letras de outros alfabetos são recusadas para que um `Х` cirílico não drible a restrição `UNIQUE`. A pontuação é removida nos dois casos, então `529.982.247-25` e `52998224725` são o mesmo cadastro — é essa grafia única que a restrição `UNIQUE` da coluna usa para impedir identificação duplicada, inclusive entre estações simultâneas. A busca normaliza o termo do mesmo jeito, por isso encontra pelo documento digitado com ou sem pontuação, e também por parte do nome (espaços repetidos não atrapalham); `%` e `_` são procurados como texto, não como curinga.

Exclusão: o hóspede com reserva (como responsável) ou com hospedagem **não é excluído**. A mensagem diz quantos vínculos existem e orienta a alterar os dados em vez de excluir, preservando o histórico (RN equivalente à do UC02). A linha do hóspede é travada (`FOR UPDATE`) antes da contagem, então nenhuma estação cria reserva ou hospedagem para ele entre a contagem e o `DELETE`. No sentido inverso, a reserva e a confirmação do check-in travam os hóspedes (`FOR KEY SHARE`) e, se algum foi excluído por outra estação, pedem para refazer o atendimento.

Alteração: o serviço compara o cadastro gravado com o que estava na tela quando o hóspede foi selecionado; se outra estação alterou antes, a gravação é recusada em vez de sobrescrever.

Integração com o UC08 e o UC10: o hóspede recém-incluído já serve à reserva e ao check-in, sem depender da carga inicial. As mensagens de "hóspede não cadastrado" não adiam mais o cadastro para a iteração 2: elas apontam a tela **Hóspedes** e, no check-in, pedem para reiniciar o atendimento depois de regularizar (atividade E da expansão do UC10 — o cadastro **não** é feito dentro do atendimento).

Na inicialização, `db/migracoes/006_hospedes.sql` é aplicado tanto a bancos novos quanto aos da iteração 1, sem alterar colunas nem apagar dados: a tabela `hospede` já tinha todas as colunas do caso de uso desde a iteração 1, e o script só cria os índices que a conferência de vínculos usa.

O UC06 segue as mesmas camadas dos demais cadastros: `Hospede`, `HospedeDAO`, `ServicoHospede`, `ControladorHospede` e `TelaHospede`. Como no UC02, `Hospede` tem dois caminhos de criação (decisão 21): `Hospede.informado(...)` valida o que foi digitado e é o único usado para gravar; o construtor do `record` apenas reconstitui a linha gravada e confere só os campos `NOT NULL`, porque os documentos fictícios da carga inicial (`11111111111`) não passam pelos dígitos verificadores e não podem derrubar a listagem nem o atendimento de quem está nelas. Os testes focados podem ser executados com `mvn test -Dtest='HospedeTest,HospedeIT'` e `mvn test -Dtest='TelasE2E#cadastroDeHospedePelaTelaPermiteUsarOHospedeNovoNoCheckInEProtegeOHistorico'`; todos usam exclusivamente `hospedabem_test`.

## Iteração 2 — UC05: itens de consumo e serviços

O administrador acessa **Cadastros → Itens de consumo e serviços** para incluir, listar, buscar por código, alterar e excluir itens. Código, descrição, categoria e preço são obrigatórios. A categoria é livre (por exemplo, frigobar, restaurante ou lavanderia). A permissão também é conferida no controlador; recepcionistas e chamadas sem sessão não podem acessar o catálogo por esse cadastro.

Decisão da equipe para o RF-11: preço **maior que zero**, até R$ 99.999.999,99, sem arredondamento de frações de centavo. `10`, `10,50` e `10.50` são aceitos; `10,501` é recusado. O código é normalizado para maiúsculas e limitado a 20 caracteres; descrição e categoria têm limites de 120 e 80 caracteres.

Na inicialização, `db/migracoes/002_itens_consumo.sql` é aplicado tanto a bancos novos quanto aos da iteração 1, sem apagar ou recarregar dados existentes. Para preparação manual, execute esse arquivo depois do esquema base. O script é idempotente e a inicialização é transacional, com bloqueio no PostgreSQL para serializar atualizações simultâneas entre estações.

A tabela `consumo` prepara a integração com o UC11: cada lançamento referencia uma conta e um item e preserva o preço unitário aplicado. Alterar o preço do catálogo não recalcula lançamentos antigos; excluir um item referenciado é recusado. **A tela e a operação de lançar consumo pertencem ao UC11 e ainda não estão implementadas.** Os testes criam lançamentos fictícios diretamente no PostgreSQL para verificar essa proteção.

O UC05 segue as mesmas camadas dos demais cadastros: `ItemConsumo`, `ItemConsumoDAO`, `ServicoItemConsumo`, `ControladorItemConsumo` e `TelaItemConsumo`. Os testes focados podem ser executados com `mvn test -Dtest=ItemConsumoIT` e `mvn test -Dtest='TelasE2E#administradorGerenciaItensDeConsumoPelaTela'`; ambos usam exclusivamente `hospedabem_test`.

## Iteração 2 — UC11: lançar consumo/serviço

**Atendimento → Lançar consumo** (recepcionista e administrador). Selecione a hospedagem ativa, veja os lançamentos da conta, escolha o item do catálogo e a quantidade (1 a 999). Cada lançamento grava data/hora, código, descrição e preço unitário aplicados, e o banco calcula o total; alterar o catálogo depois não muda lançamentos antigos. Só são aceitos lançamentos em hospedagem ATIVA com conta ABERTA. Camadas: `Consumo` / `Hospedagem.lancarConsumo` → `ConsumoDAO` → `ServicoConsumo` → `ControladorConsumo` → `TelaConsumo`.

A migração `db/migracoes/003_consumo.sql` (sem apagar dados) acrescenta a `consumo`: `valor_total` como coluna gerada pelo banco (`preco_unitario * quantidade`, `NUMERIC(13,2)`, que comporta o maior lançamento aceito), o limite de 999 na quantidade, `funcionario_id` e o código e a descrição do item cobrados, preenchidos para lançamentos antigos. Como `ALTER TABLE` trava a tabela inteira, o `InicializadorBanco` só executa o script enquanto `valor_total` ainda não é coluna gerada; reiniciar uma estação não bloqueia os lançamentos das outras. O lançamento trava só as linhas da hospedagem e da conta (`SELECT ... FOR UPDATE OF h, c`), a mesma trava usada pelo check-out (UC12), e não relê o quarto nem os lançamentos anteriores.

Testes: `mvn test -Dtest='ConsumoIT,ConsumoDominioTest'` e `TelasE2E#recepcionistaLancaConsumoPelaTela`.

## Iteração 2 — UC12: realizar check-out

**Atendimento → Check-out**, em três passos numa tela (RNF-12): localizar a hospedagem, conferir diárias + consumos + total, escolher a forma (dinheiro, pix, cartão de crédito ou débito) e confirmar. Antes da confirmação nada é gravado. A confirmação, numa única transação, grava o pagamento e a data/hora de saída, fecha a conta, encerra a hospedagem e deixa o quarto DISPONIVEL. O total é recalculado na confirmação; se diferir do conferido (consumo lançado ou virada de dia), a operação é recusada.

**Regra de diárias (decisão da equipe, `RegraDiarias`):** conta-se o número de noites — dias de calendário entre a data de entrada e a data da saída efetiva, sem considerar horário — com mínimo de uma diária. Saída no mesmo dia cobra 1; saída antecipada cobra só as noites usadas; atraso cobra as noites efetivas, sem multa nem acréscimo por horário. O valor da diária é o preservado na conta no check-in.

**Concorrência:** lançamento e check-out travam as mesmas linhas (`hospedagem`, `conta_hospedagem` e, no check-out, `quarto`). Um consumo que chega durante o check-out espera e encontra a conta fechada; uma segunda confirmação encontra a hospedagem encerrada, e o `UNIQUE` de `pagamento` impede cobrança duplicada.

**Relógio:** a data/hora de entrada, de saída e dos lançamentos vem do relógio da estação alinhado ao do banco (`App.relogio`, que mede a diferença na inicialização). Assim, uma estação com o relógio adiantado ou atrasado não produz saída "anterior à entrada" nem muda a contagem de diárias perto da meia-noite.

A migração `db/migracoes/004_checkout.sql` acrescenta `conta_hospedagem.fechada_em` e a tabela `pagamento`, com valores em `NUMERIC(15,2)`; acima disso o extrato recusa com mensagem de negócio. Como a 003, ela só é executada enquanto falta.

Testes: `mvn test -Dtest='CheckOutIT,CheckOutDominioTest'` e `TelasE2E#recepcionistaLancaConsumoEConcluiCheckOutPelasTelas`.

## Iteração 2 — RNF-8: troca e renovação de senha

**Política (decisão da equipe, `PoliticaDeSenha`):** ao menos 8 caracteres, com letra e número; validade de **90 dias** a partir de `senha_alterada_em` (vence exatamente ao completar o prazo); a nova senha deve ser confirmada e diferente da atual. Senha definida pelo administrador no cadastro ou na redefinição é **provisória** e deve ser trocada no primeiro acesso.

Com a senha vencida, o login leva direto à tela **Trocar senha**, que também tem **Sair**; até a troca, todos os controladores recusam operações (inclusive chamadas diretas), exceto a própria troca. O vencimento é conferido a cada operação, então uma senha que completa 90 dias com a sessão aberta também passa a bloquear. A redefinição pelo administrador vale a partir do próximo login do funcionário: uma sessão já aberta em outra estação não é encerrada (o sistema não revoga sessões). Espaços não contam no mínimo de 8 caracteres. A troca também está no menu lateral. Só o hash PBKDF2 é gravado. A migração `db/migracoes/005_senha.sql` acrescenta `funcionario.senha_provisoria`. Em bancos existentes, contas cuja `senha_alterada_em` tem mais de 90 dias serão obrigadas a trocar a senha no próximo login.

Testes: `mvn test -Dtest='TrocaSenhaIT,PoliticaDeSenhaTest'`.

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
| Reserva alterável ou cancelável, cancelamento e dados da alteração (UC09) | `Reserva.exigirAlteravel`, `Reserva.cancelar`, `Reserva.alteradaPara` |
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
| Unitário | `*Test` (87 testes) | Todas as regras do `model` (`Atendimento`, `Quarto`, `Reserva`, `SolicitacaoReserva`, `TipoQuarto`, `Periodo`, `Ocupacao`, `Disponibilidade`, `Funcionario`, `PoliticaDeSenha`), `Sessao`, hash de senha, tradução de duplicidade simultânea, mensagens de erro, conversão de campos | não usa |
| Integração | `*IT` (138 testes) | Controladores + services + DAOs + PostgreSQL: regras dos UCs entregues, transação do check-in, falha injetada na gravação, concorrência entre conexões, reinício | `hospedabem_test` |
| Ponta a ponta (E2E) | `TelasE2E` (11 testes) | Telas JavaFX reais operadas como um usuário (`RoboFx`): login, permissões, cadastros, funcionários, hóspedes, disponibilidade, reserva, alteração e cancelamento de reserva, check-in com e sem reserva, consumo e check-out. Confere a tela e o banco. | `hospedabem_test` |
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
| Iteração 2 (UC06), 09/11/2026 | OpenJDK 25 com `--release 17`, PostgreSQL 17.11 | **207**, 0 falhas, 0 erros (79 unitários, 118 de integração, 10 E2E) | **539 mutantes, 516 mortos (96%)** |
| Iteração 2 (UC09), 13/11/2026 | OpenJDK 25 com `--release 17`, PostgreSQL 17.11 | **236**, 0 falhas, 0 erros (87 unitários, 138 de integração, 11 E2E) | **537 mutantes, 513 mortos (96%)** |

O build exige no mínimo 95% (`mutationThreshold`). Na rodada do UC09 restaram 24 sobreviventes, nenhum no código do caso de uso entregue (`Reserva`, `ReservaDAO`, `ServicoReserva` e `ControladorReserva` ficaram sem sobreviventes). Eles se dividem assim:

- 4 remoções de `TipoQuartoDAO.bloquear` nos cadastros de tipo e quarto. O bloqueio só faz diferença com transações concorrentes. O bloqueio equivalente da reserva é verificado por `ConcorrenciaIT`, inclusive o do tipo de destino na alteração do UC09, e o do UC02 (`FuncionarioDAO.bloquearAdministradores`) também — então estes quatro são cobríveis do mesmo jeito e ficaram para uma próxima iteração, não por serem intestáveis.
- 4 remoções de guardas repetidas em dois níveis em `Hospedagem` e `ContaDaHospedagem` (`exigirAtiva`, `exigirAberta`): com a guarda de um nível removida, a do outro ainda recusa, e a mensagem é a mesma.
- 4 de `ServicoAutenticacao.trocarSenha`: três remoções de `Senha.apagar` (limpeza de memória, sem efeito observável) e um `return null` idêntico ao original.
- `PBEKeySpec.clearPassword()`: limpeza de memória, sem efeito observável.
- `SET LOCAL statement_timeout = 3000` da consulta de disponibilidade: só se manifesta com uma consulta lenta.
- 2 corpos de transação de `ServicoFuncionario.alterar` e `ServicoHospede.alterar` com o retorno substituído: as lambdas já devolvem `null` ou um valor ignorado, então os mutantes são idênticos ao original.
- `FormaPagamento.toString` com retorno vazio.
- 7 sem cobertura, todos do UC11, do UC12 e da RNF-8: corpos de transação cujo retorno não é usado (`ServicoCheckOut`, `ServicoAutenticacao`), `listarHospedagensAtivas` e os `toString`/`descricao` de `ItemConsumo` e `FormaPagamento`. São lacunas reais daqueles casos de uso, não do UC09, e ficam registradas aqui para a próxima iteração.

### O que a mutação revelou

- A primeira rodada do PIT (80%) revelou lacunas reais, cobertas depois por testes: etapas do check-in fora de ordem, manutenção do quarto após o resumo com folga no tipo, reserva ou quarto apagados antes da confirmação, capacidade exatamente no limite e senha não apagada da memória. Revelou também duas pré-checagens de duplicidade redundantes com a restrição `UNIQUE` do banco, que foram removidas. Depois da separação das regras no `model`, a mutação apontou mais uma lacuna, já coberta: check-in sem reserva quando a última vaga do tipo pertence à reserva de outro hóspede. Com a criação da camada `service`, apontou `ControladorReserva.buscarResponsavel` sem teste, coberto por `ReservaIT.buscarResponsavelRetornaOHospedeCadastrado`.
- No UC02, a mutação apontou 5 lacunas reais, todas cobertas depois: os campos opcionais em branco (estado, telefone e e-mail) podiam virar texto vazio em vez de `null` sem nenhum teste reclamar; o limite exato de 120 caracteres do nome não era exercitado; e `ServicoFuncionario.alterar` conferia a data de contratação sem que nenhum teste cobrisse data futura **na alteração** — só na inclusão.
- Depois da revisão do UC02, a mutação mostrou que as duas pré-checagens de duplicidade do `ServicoFuncionario` podiam ser removidas sem nenhum teste falhar: com a tradução da violação do `UNIQUE` já implementada, a mensagem é a mesma pelos dois caminhos. As pré-checagens foram removidas, como já havia acontecido com as dos outros cadastros na iteração 1 (decisão 23).
- No UC09, a mutação apontou duas lacunas reais, cobertas depois: o bloqueio do **tipo de destino** na alteração sobrevivia porque a corrida de duas remarcações para a mesma vaga rodava uma única vez — a janela é estreita, e o teste passou a repeti-la em 10 rodadas, cada uma num bloco de dias próprio; e o caminho da reserva **apagada por fora** entre a consulta da tela e a gravação não era exercitado por nenhum teste (`ReservaIT.reservaApagadaEntreAConsultaEAGravacaoEhInformada`).
- Os E2E encontraram um problema de interface: o item do menu e o botão da página de reserva tinham o mesmo texto. O botão passou a se chamar "Confirmar reserva". As capturas estão em `docs/telas`.

## Pipeline dos Pull Requests

O workflow [CI](.github/workflows/ci.yml) executa automaticamente nos PRs destinados à `main` e nos pushes para a `main`. Depois que estiver integrado, também poderá ser iniciado manualmente pela aba **Actions → CI → Run workflow**.

As verificações usam Ubuntu 24.04, Java 17 e cache Maven, nesta ordem:

| Check | Verificações |
|---|---|
| **Lint** | actionlint 1.7.12 valida os workflows; Checkstyle valida os fontes e testes Java. |
| **Build e testes** | Etapas separadas para unitários (`*Test`), integração (`*IT`) e interface JavaFX (`*E2E`), seguidas da geração do JAR. |
| **Mutação (PIT)** | Executa os testes contra mutações do domínio, controladores e serviços. Falha se a pontuação ficar abaixo de **95%**, conforme o `pom.xml`. |

Os testes e a mutação usam bancos PostgreSQL 17 temporários e isolados por job, sempre chamados `hospedabem_test`. As telas rodam em uma sessão gráfica virtual com Xvfb. O PIT exclui os E2E e usa uma única thread para não disputar o banco entre mutantes. Não são necessários secrets: as credenciais declaradas no workflow pertencem somente aos bancos temporários da execução.

O lint Java verifica imports não utilizados ou redundantes, instruções vazias, correspondência entre classe e arquivo, tabulações, espaços no final da linha e quebra de linha final. As regras estão em [config/checkstyle.xml](config/checkstyle.xml); violações falham o build. O Checkstyle também roda na fase `validate` do Maven, portanto vale para os comandos locais de build e testes.

Para executar as mesmas verificações localmente, com o banco de teste disponível e uma sessão gráfica para os E2E:

```bash
actionlint .github/workflows/ci.yml
mvn checkstyle:check
mvn clean test -Dtest='*Test'
mvn test -Dtest='*IT'
mvn test -Dtest='*E2E' -Dhb.e2e.capturas=target/e2e-capturas
mvn -DskipTests package
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Em Linux sem monitor, execute o comando dos E2E com o prefixo `xvfb-run --auto-servernum`. Não execute os testes de integração e a mutação simultaneamente contra o mesmo banco.

Para acompanhar, abra o PR e consulte **Checks**, ou vá à aba **Actions** do repositório. Na página da execução, a seção **Artifacts** disponibiliza `lint-java`, `resultados-testes-linux` (relatórios e capturas) e `mutacao-pit` (HTML e XML), inclusive quando a respectiva verificação falha e há arquivos gerados. Quando o build passa, também fica disponível `hospedabem-linux-java17`, com `hospedabem.jar`. Os arquivos são mantidos por sete dias.

O JAR gerado inclui os componentes nativos do JavaFX para **Linux**; para Windows ou macOS, faça o build no respectivo sistema conforme as instruções de execução. A entrega automatizada nesta pipeline é o artefato para download, sem deploy em servidor. A execução só fica aprovada quando os três checks passam; não há `continue-on-error` nem redução do limite de mutação no workflow.

O resultado aparece no PR. Para tornar sua aprovação obrigatória antes do merge, configure uma regra de proteção da `main` exigindo **Lint**, **Build e testes** e **Mutação (PIT)**.

## Verificação: casos de uso × telas × testes

| UC | Tela | Controlador | Testes |
|---|---|---|---|
| UC01 | `TelaLogin`, `TelaPrincipal` (itens de cadastro desabilitados para a recepcionista) | `ControladorAutenticacao`, `Sessao` | `AutenticacaoIT` (login correto, senha incorreta, matrícula inexistente, chamadas diretas sem sessão e sem perfil, hash PBKDF2, senha apagada da memória), `SenhaTest`, `SessaoTest`, `TelasE2E` |
| UC02 | `TelaFuncionario` (lista, busca por matrícula/nome/CPF, formulário com perfil e senha inicial) | `ControladorFuncionario`, `ServicoFuncionario` | `FuncionarioTest` (campos obrigatórios, CPF com dígitos verificadores, UF, telefone, e-mail, autoexclusão, rebaixamento do próprio perfil), `PoliticaDeSenhaTest`; `ServicoFuncionarioTest` (tradução da violação do `UNIQUE` em gravações simultâneas); `FuncionarioIT` (inclusão/listagem/busca/alteração/exclusão, matrícula e CPF duplicados na inclusão e na alteração com dados preservados, acesso de recepcionista e sem sessão em todas as operações, delegação do perfil de administrador, autoexclusão, auto-rebaixamento, sessão rebaixada ou excluída por outra estação, matrícula em qualquer caixa no login, linha fora do formato antigo ainda listada e autenticando, campo maior que a coluna, exclusão com reserva e com hospedagem, somente hash gravado, login do funcionário criado, redefinição de senha, senha apagada da memória, persistência após reinício); `ConcorrenciaIT` (dois administradores se rebaixando ao mesmo tempo); `TelasE2E` (cadastro com a data já preenchida, duplicidade, delegação de perfil, recusa de senha no "Salvar", impedimento de exclusão e login do recém-cadastrado) |
| UC03 | `TelaTipoQuarto` | `ControladorTipoQuarto` | `TelasE2E`, `DominioTest`, `CadastroIT`: inclusão/busca/alteração/exclusão, código duplicado, valores inválidos, exclusão com quarto ou reserva, redução de capacidade abaixo de reserva |
| UC04 | `TelaQuarto` | `ControladorQuarto` | `TelasE2E`, `DominioTest`, `CadastroIT`: número duplicado, tipo inexistente, ocupado/reservado pelo cadastro, RN-Q04 (hospedagem ativa), RN-Q05 (manutenção/troca de tipo/exclusão inviabilizando reserva, com dados preservados), RN-Q06 (histórico) |
| UC05 | `TelaItemConsumo` | `ControladorItemConsumo` | `ItemConsumoIT`, `TelasE2E`: CRUD, normalização, duplicidade, campos e preço, autorização, item inexistente, histórico de consumo, migração e reinício |
| UC06 | `TelaHospede` (lista, busca por documento ou nome, formulário com os opcionais) | `ControladorHospede`, `ServicoHospede` | `HospedeTest` (CPF com dígitos verificadores, passaporte inclusive só numérico, letras de outros alfabetos recusadas, telefone internacional, normalização, campos obrigatórios e opcionais, UF, telefone, e-mail, limites das colunas, data de nascimento futura ou absurda, linha da carga inicial reconstituída); `HospedeDAOTest` (normalização do documento); `HospedeIT` (CRUD pela recepcionista, opcionais ausentes gravados como `NULL`, CPF e passaporte, identificação inválida e duplicada na inclusão e na alteração com dados preservados, hóspede inexistente, chamadas sem sessão em todas as operações e os dois perfis aceitos, exclusão recusada com reserva e com hospedagem, hóspede novo usado na reserva e no check-in, mensagens do UC08 e do UC10 e reinício do atendimento, migração dos índices sem perder dados, exclusão concorrente com reserva em andamento, acompanhante excluído antes da confirmação do check-in, alteração sobre cadastro desatualizado, busca com pontuação e curinga, estrangeiro da carga alterado); `TelasE2E` (inclusão, duplicidade, busca com pontuação, uso no check-in e impedimento de exclusão) |
| UC07 | `TelaDisponibilidade` | `ControladorDisponibilidade`, `ServicoDisponibilidade` | `TelasE2E`, `OcupacaoTest` (período inválido, sobreposição, adjacência, reservas que não se sobrepõem entre si); `DisponibilidadeIT` (manutenção, convertida não contada duas vezes, cancelada, hospedagem atrasada) |
| UC08 | `TelaReserva` | `ControladorReserva` | `TelasE2E`, `ReservaIT` (válida com funcionário da sessão, capacidade, indisponibilidade, responsável/tipo/período inválidos); `ConcorrenciaIT` (última vaga) |
| UC09 | `TelaAlteracaoReserva` (localizar pelo código, dados e situação, alteração e cancelamento com confirmação) | `ControladorReserva`, `ServicoReserva` | `DominioTest` (convertida e cancelada não alteráveis nem canceláveis, cancelamento preservando os dados, alteração devolvida sem mudar a gravada, alteração sem mudança, capacidade no limite exato, entrada mantida no passado × remarcada para trás); `ReservaIT` (consulta com dados e situação, código inexistente, alteração de responsável/tipo/datas/quantidade com `versao` incrementada, própria vaga mantida em tipo com um só quarto, mudança de tipo consumindo o destino e devolvendo a origem, indisponibilidade no destino e no período novo sem gravar, cancelamento persistido com registro preservado e capacidade devolvida, convertida e já cancelada recusadas, reserva desatualizada recusada na alteração e no cancelamento, responsável/tipo/período/quantidade inválidos, responsável excluído por outra estação, os dois perfis e sem sessão); `ConcorrenciaIT` (alteração × check-in da mesma reserva em 5 rodadas, duas remarcações para a última vaga); `CheckInIT` (alteração e cancelamento reais entre o resumo e a confirmação); `TelasE2E` (localizar, recusa de capacidade, alteração, desistência e confirmação do cancelamento, cancelada não alterável, capacidade de volta na disponibilidade) |
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
12. **Check-in com reserva** só é aceito de hoje até a saída prevista. A hospedagem vai de hoje até a saída da reserva. Check-in antecipado exige alterar a reserva pelo UC09, entregue nesta iteração. **Sem reserva**, a data de entrada informada precisa ser hoje, pois o check-in registra a entrada efetiva.
13. **O responsável deve estar entre os participantes** (fluxo principal, passo 3) e cada hóspede é contado uma vez.
14. **Mudança de reserva entre o resumo e a confirmação** é detectada pela coluna `versao`, incrementada a cada atualização, e pela comparação de situação, tipo, período, quantidade e responsável.
15. **Hóspede não cadastrado:** a mensagem aponta a tela **Hóspedes** (UC06). Na reserva, basta cadastrar e registrar em seguida; no check-in, o atendimento é **reiniciado** depois de regularizar o cadastro, conforme a atividade E da expansão do UC10 — o cadastro não é feito dentro do atendimento.
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
26. **Reserva já cancelada (UC09).** Os artefatos não dizem o que acontece ao alterar ou cancelar uma reserva que já está `CANCELADA`. A equipe decidiu **recusar** as duas ações, e não tratar o cancelamento como idempotente: a tela mostra a situação antes de agir, então repetir a ação indica tela desatualizada, e a recusa nomeia a situação em vez de confirmar silenciosamente um engano. Para retomar a estadia, registra-se uma reserva nova, que passa pela disponibilidade do momento. A recusa não incrementa a `versao`. Mesma decisão vale para a `CONVERTIDA`, que já virou hospedagem. Coberto por `ReservaIT.reservaJaCanceladaNaoEhAlteradaNemCanceladaDeNovo` e `reservaConvertidaNaoEhAlteradaNemCancelada`.
27. **Fonte dos diagramas:** a implementação segue as exportações `GrupoN_DC.jpg` e `GrupoN_DCP.jpg` do projeto Astah do grupo (`GrupoN.asta`).

## Transações, concorrência e falhas

- **Contrato de `confirmarCheckIn()`:** uma única transação busca e bloqueia o quarto e revalida tudo. Em seguida, cria a hospedagem com novo código, entrada atual, saída prevista e situação ativa, associa os hóspedes e o quarto, ocupa o quarto, abre exatamente uma conta e converte a reserva. Qualquer exceção faz rollback total, e o sucesso só é informado após o commit. `CheckInIT.falhaDuranteAGravacaoDesfazTudo` injeta falhas por *trigger* no meio e no último passo.
- **Concorrência entre estações:** toda operação que altera a disponibilidade de um tipo bloqueia a linha do tipo com `SELECT ... FOR UPDATE`, em ordem de id. São elas: reserva, alteração de reserva (os dois tipos envolvidos, no mesmo `bloquear`), check-in, inclusão, alteração e exclusão de quarto, e alteração de tipo. O cancelamento de reserva é a exceção deliberada: só devolve capacidade, e trava apenas a própria reserva. Isso serializa reservas e check-ins do mesmo tipo entre processos diferentes, e não depende de bloqueio Java. O quarto também é bloqueado na confirmação.
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

- RNF-8: troca de senha pelo próprio funcionário e exigência de renovação periódica. O UC02 já entregou a definição e a redefinição da senha, a coluna `senha_alterada_em` atualizada a cada troca e a `PoliticaDeSenha`; falta definir o prazo de validade e bloquear as operações até a renovação.
- UC05, UC09, UC11 e UC12, conforme a listagem de casos de uso.

## Entrega em ZIP

`GrupoN_Iteracao1.zip` contém **somente** a pasta `src/`. Instruções para quem recebe só o ZIP estão em [src/LEIA-ME.md](src/LEIA-ME.md). Para gerar o ZIP:

```bash
rm -f GrupoN_Iteracao1.zip && zip -r -X GrupoN_Iteracao1.zip src -x '*.DS_Store' && unzip -l GrupoN_Iteracao1.zip
```
