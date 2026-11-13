# Expansão dos casos de uso — iteração 2

Complemento à expansão da primeira iteração, com os nomes de UC da listagem da atividade D e as regras implementadas em `e354fcf`. Administrador herda as ações de atendimento do funcionário. Toda ação exige sessão e senha válida; erros de validação/permissão são apresentados na própria interface e não gravam resultado parcial.

## UC02 — Gerenciar Funcionário

**Ator:** administrador. **Referências:** RF-2, RF-12, RNF-7/8/10. **Pré-condição:** sessão administrativa com senha válida.

1. Abrir Funcionários; listar ou buscar um cadastro.
2. Informar matrícula, CPF, nome, contatos, contratação, perfil e senha inicial no cadastro; na alteração, selecionar o cadastro existente.
3. Salvar e receber confirmação; opcionalmente redefinir senha, delegar perfil ou excluir.

**Alternativas/exceções:** dados inválidos ou duplicados são recusados; vínculos históricos impedem exclusão. A equipe impede autoexclusão, autorrebaixamento e remoção do último administrador. Uma redefinição torna a senha provisória. Operações de gravação reconferem no banco a permissão do autor, protegendo contra rebaixamento concorrente. **Pós-condição:** cadastro persistido, ou nenhuma alteração em caso de erro.

## UC05 — Gerenciar Item de Consumo/Serviço

**Ator:** administrador. **Referências:** RF-3, RF-11. **Pré-condição:** sessão administrativa com senha válida.

1. Abrir Itens de consumo e serviços; listar ou buscar por código.
2. Informar código, descrição, categoria e preço; salvar a inclusão/alteração.
3. Receber confirmação; excluir somente item sem consumo associado.

**Alternativas/exceções:** código duplicado, preço não positivo ou com fração de centavo e campos inválidos são recusados. **Pós-condição:** catálogo atualizado; lançamentos anteriores mantêm descrição e preço aplicados. Limites de preço e normalização são decisões da equipe descritas no README, não novos requisitos da disciplina.

## UC06 — Gerenciar Hóspede

**Ator:** funcionário. **Referências:** RF-5, RF-8. **Pré-condição:** sessão com senha válida.

1. Abrir Hóspedes; listar ou buscar por documento/nome.
2. Informar CPF/passaporte, nome, nascimento, telefone e opcionais; salvar inclusão/alteração.
3. Receber confirmação; utilizar o cadastro na reserva/check-in.

**Alternativas/exceções:** identificação inválida ou duplicada, campos inválidos e cadastro alterado por outra estação são recusados. Exclusão com reserva ou hospedagem é recusada para preservar histórico. Se o hóspede faltar durante um check-in, cadastrar nesta tela e reiniciar o atendimento, conforme a expansão anterior do UC10. **Pós-condição:** cadastro disponível aos atendimentos ou histórico intacto em caso de recusa.

## UC09 — Consultar, Alterar e Cancelar Reserva

**Ator:** funcionário. **Referência:** RF-16. **Pré-condição:** sessão com senha válida; reserva previamente registrada.

1. Abrir Alterar/cancelar reserva e localizar pelo código.
2. Conferir responsável, tipo, período, quantidade e situação.
3. Informar alterações e salvar; o sistema revalida cadastro, capacidade e disponibilidade.
4. Receber confirmação com a reserva atualizada.

**Alternativa de cancelamento:** escolher Cancelar e confirmar o diálogo; situação passa a `CANCELADA` e a capacidade é devolvida à disponibilidade. Desistir do diálogo não altera o banco.

**Exceções:** reserva inexistente, `CANCELADA`, `CONVERTIDA` ou desatualizada é recusada. A própria vaga é excluída da recontagem. Entrada mantida no passado é admitida; remarcação para entrada passada é recusada, conforme decisão da equipe. **Pós-condição:** reserva atualizada com versão incrementada ou cancelada sem apagar histórico; concorrência com check-in não permite ambos sobre o mesmo estado.

## UC11 — Lançar Consumo

**Ator:** funcionário. **Referências:** RF-6, RF-19. **Pré-condição:** sessão com senha válida; hospedagem ativa e conta aberta; item cadastrado.

1. Abrir Lançar consumo e selecionar hospedagem ativa.
2. Conferir os lançamentos, selecionar item e informar quantidade.
3. Confirmar lançamento; o sistema relê o item do catálogo, valida quantidade e grava os dados aplicados com data/hora e funcionário.
4. Receber confirmação do valor lançado e visualizar a conta atualizada.

**Exceções:** item inexistente, quantidade fora de 1–999 e hospedagem encerrada/conta fechada são recusados. Se um check-out estiver fechando a mesma conta, a operação espera a trava e é recusada após o fechamento. **Pós-condição:** um consumo persistido; valores históricos não dependem de edições futuras do catálogo.

## UC12 — Realizar Check-out

**Ator:** funcionário. **Referências:** RF-6, RF-20–23, RNF-12. **Pré-condição:** sessão com senha válida; hospedagem ativa e conta aberta.

1. Abrir Check-out e selecionar hospedagem ativa.
2. Conferir diárias, consumos e total calculados para a saída atual.
3. Escolher dinheiro, pix, cartão de crédito ou cartão de débito.
4. Confirmar pagamento no diálogo.
5. O sistema recalcula o total e, em uma transação, grava pagamento/saída, fecha a conta, encerra hospedagem e libera quarto.

**Alternativas/exceções:** sem forma, informar erro; ao recusar o diálogo, manter conta aberta e quarto ocupado. Se total mudou desde a conferência, exigir nova conferência. Pagamento duplicado e consumo posterior ao fechamento são recusados. Falha em qualquer gravação desfaz todas as alterações. **Pós-condição:** um pagamento, conta fechada, hospedagem encerrada e quarto disponível; sem confirmação, continua pendente, sem gravar pagamento.

São uma página e um diálogo, dentro das cinco telas do RNF-12. O extrato exibido é conferência em tela; impressão do UC14 permanece na iteração 3.

## Renovar senha — extensão do UC01 / RNF-8

**Ator:** funcionário autenticado. Após login com senha provisória ou vencida, a única continuação operacional permitida é a troca da própria senha. Informar senha atual, nova e confirmação; o sistema verifica a atual, exige ao menos oito caracteres sem contar espaços, letra e número, confirmação igual e nova senha diferente da atual. Salva somente hash e instante da troca, remove o estado provisório e renova a validade por 90 dias. Com erro ou alteração concorrente da senha, não grava e pede correção/nova tentativa.

O prazo, a composição e a senha provisória são decisões da equipe para concretizar o RNF-8. Não foram extraídos como números obrigatórios do documento de requisitos.
