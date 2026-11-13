# Rastreabilidade da iteração 2

Fontes: atividade C, `GrupoN_DocRequisitos_v3.docx`, revisão 2.2, seção 3; atividade D, `GrupoN_ListagemCasosDeUso_.docx`, seções 3–4, fornecidas em `GrupoN-APSOO`. Os códigos RF abaixo seguem o mapeamento da atividade D. A iteração 1 entregou UC01/03/04/07/08/10; a segunda acrescenta UC02/05/06/09/11/12 e RNF-8.

Os nomes de classes e métodos foram conferidos no código. A coluna de testes aponta cobertura existente, sem representar uma nova execução. A rodada local anterior à #10 registrou 246 testes aprovados e 96% PIT; ambiente, comandos, resultados e limitações estão em [validacao-iteracao-2.md](validacao-iteracao-2.md). A situação de cada execução Linux é consultada no [CI](https://github.com/SamuelNahas/hospedabem/actions/workflows/ci.yml), não inferida desses resultados locais.

Caminhos: telas em `src/main/java/hospedabem/view/`, controladores em `src/main/java/hospedabem/controller/`; testes de integração em `src/test/java/hospedabem/controller/` e E2E em `src/test/java/hospedabem/view/TelasE2E.java`.

| UC / iteração | Requisitos funcionais | RNFs relacionados | Tela e controlador | Evidência no código de testes |
|---|---|---|---|---|
| UC01 / 1 — autenticar | RF-1, RF-7 | RNF-7, RNF-8, RNF-10 | `TelaLogin`, `TelaPrincipal`; `ControladorAutenticacao` | `AutenticacaoIT`, `SessaoTest`, `SenhaTest`; `TelasE2E.loginRecusaSenhaErradaERecepcionistaNaoAcessaCadastros` |
| UC02 / 2 — funcionário | RF-2, RF-12 | RNF-7, RNF-8, RNF-10 | `TelaFuncionario`; `ControladorFuncionario` | `FuncionarioTest`, `FuncionarioIT`, `ServicoFuncionarioTest`; `TelasE2E.administradorCadastraFuncionarioDelegaPerfilEORecemCadastradoEntraNoSistema` |
| UC03 / 1 — tipo de quarto | RF-3, RF-9 | RNF-10 | `TelaTipoQuarto`; `ControladorTipoQuarto` | `DominioTest`, `CadastroIT`; `TelasE2E.administradorCadastraTipoEQuartoEConsultaDisponibilidade` |
| UC04 / 1 — quarto | RF-3, RF-10 | RNF-10 | `TelaQuarto`; `ControladorQuarto` | `DominioTest`, `CadastroIT`; `TelasE2E.administradorCadastraTipoEQuartoEConsultaDisponibilidade` |
| UC05 / 2 — item/serviço | RF-3, RF-11 | RNF-10 | `TelaItemConsumo`; `ControladorItemConsumo` | `ItemConsumoIT`; `TelasE2E.administradorGerenciaItensDeConsumoPelaTela` |
| UC06 / 2 — hóspede | RF-5, RF-8 | RNF-10 | `TelaHospede`; `ControladorHospede` | `HospedeTest`, `HospedeDAOTest`, `HospedeIT`; `TelasE2E.cadastroDeHospedePelaTelaPermiteUsarOHospedeNovoNoCheckInEProtegeOHistorico` |
| UC07 / 1 — disponibilidade | RF-13 | RNF-2, RNF-10 | `TelaDisponibilidade`; `ControladorDisponibilidade` | `OcupacaoTest`, `DisponibilidadeIT`, `LimitesRnfIT`; `TelasE2E.administradorCadastraTipoEQuartoEConsultaDisponibilidade` |
| UC08 / 1 — reserva | RF-6, RF-14, RF-15 | RNF-1, RNF-10 | `TelaReserva`; `ControladorReserva` | `ReservaIT`, `ConcorrenciaIT`; `TelasE2E.recepcionistaRegistraReservaPelaTela` |
| UC09 / 2 — consultar/alterar/cancelar | RF-16 | RNF-1, RNF-10 | `TelaAlteracaoReserva`; `ControladorReserva` | `DominioTest`, `ReservaIT`, `ConcorrenciaIT`, `CheckInIT`; `TelasE2E.recepcionistaAlteraECancelaReservaPelaTela` |
| UC10 / 1 — check-in | RF-6, RF-17, RF-18 | RNF-1, RNF-4, RNF-10, RNF-12 | `TelaCheckIn`; `ControladorCheckIn` | `AtendimentoTest`, `DominioTest`, `CheckInIT`, `ConcorrenciaIT`, `PersistenciaIT`; `TelasE2E.checkInComReservaPelaTelaOcupaQuartoEAbreConta`, `checkInSemReservaPelaTelaRespeitaCapacidade` |
| UC11 / 2 — consumo | RF-6, RF-19 | RNF-1, RNF-4, RNF-10 | `TelaConsumo`; `ControladorConsumo` | `ConsumoDominioTest`, `ConsumoIT`; `TelasE2E.recepcionistaLancaConsumoEConcluiCheckOutPelasTelas` |
| UC12 / 2 — check-out/pagamento | RF-6, RF-20, RF-21, RF-22, RF-23 | RNF-1, RNF-4, RNF-10, RNF-12 | `TelaCheckOut`; `ControladorCheckOut` | `CheckOutDominioTest`, `CheckOutIT`, `PersistenciaIT.checkOutSobreviveAoReinicio`; `TelasE2E.recepcionistaLancaConsumoEConcluiCheckOutPelasTelas` |
| RNF-8 / 2 — renovar senha | suporte a RF-1, RF-2, RF-7 | RNF-7, RNF-8, RNF-10 | `TelaTrocaSenha`; `ControladorAutenticacao`, `Sessao` | `PoliticaDeSenhaTest`, `TrocaSenhaIT`, `MigracaoIteracaoUmIT`; `TelasE2E.cicloCompletoDaIteracaoDoisPelasTelas` |

RF-7 (mensagem em acesso não autorizado) e RNF-10 são transversais: os controladores recusam chamadas sem sessão/perfil, além da restrição visual. RNF-1 e RNF-14 são apoiados por JDBC/PostgreSQL; RNF-11, pela organização em camadas. A coluna RNFs descreve relação com a cobertura, não conformidade integral com todos os aspectos de implantação.

A validação integrada é `TelasE2E.cicloCompletoDaIteracaoDoisPelasTelas`. `MigracaoIteracaoUmIT` compara atualização/instalação nova e opera sobre dados antigos; `LimitesRnfIT` mede limites locais de RNF-1/RNF-2. RNF-12: check-in com quatro etapas na mesma página; check-out com uma página e um diálogo (duas telas). RNF-3 (15 estações), RNF-5 (backup/restauração), RNF-6 (24×7), rede degradada e Windows não têm aprovação de execução nesta entrega.

## Iteração 3 — ainda sem implementação

| UC | Entrega prevista | RFs | Estado |
|---|---|---|---|
| UC13 | Comprovante impresso de reserva | RF-24 | sem tela/teste de impressão |
| UC14 | Extrato impresso da conta | RF-25 | o resumo da tela de check-out não é impressão |
| UC15 | Relatório de ocupação | RF-4, RF-26 | sem tela/teste de relatório |
| UC16 | Relatório de reservas | RF-4, RF-27 | sem tela/teste de relatório |
| UC17 | Hospedagens por hóspede | RF-4, RF-28 | sem tela/teste de relatório |
| UC18 | Relatório de faturamento | RF-4, RF-29 | sem tela/teste de relatório |
