# GitHub básico para o grupo HospedaBem

Este guia mostra como escolher uma tarefa, assumir a responsabilidade, acompanhar o board, criar uma branch, enviar alterações e abrir um Pull Request (PR). Use o site do GitHub para organizar o trabalho e o GitHub Desktop para enviar o código.

## 1. Onde fica cada coisa

Abra o [repositório HospedaBem](https://github.com/SamuelNahas/hospedabem).

| Lugar | Para que serve |
|---|---|
| **Code** | Arquivos do projeto e seletor de branches. O README explica como executar e testar. |
| **Issues** | Tarefas, critérios de aceitação, responsáveis e comentários. |
| **Pull requests** | Alterações propostas para revisão antes de entrarem na versão principal. |
| **Projects** | Acesso aos projetos vinculados ao repositório. |
| **Board da iteração 2** | Cartões organizados em A fazer, Em andamento e Concluído. |
| **Milestone / marco** | Agrupa as issues da mesma entrega: Iteração 2. |

Links para salvar:

- [Board da iteração 2](https://github.com/users/SamuelNahas/projects/3).
- [Todas as issues](https://github.com/SamuelNahas/hospedabem/issues).
- [Issues da iteração 2](https://github.com/SamuelNahas/hospedabem/milestone/1).
- [Pull Requests](https://github.com/SamuelNahas/hospedabem/pulls).

O repositório é público, mas o board foi criado como privado. Se o código abrir e o board não, peça ao Samuel acesso ao projeto. Para enviar código ao repositório, aceite também o convite de colaborador. Acesso ao repositório e acesso ao projeto são configurações separadas.

### Palavras que vamos usar

| Termo | Significado prático |
|---|---|
| **Issue / task** | Um trabalho a realizar, identificado por um número, por exemplo `#4`. O board também tem histórias de usuário; elas são tarefas de execução para a divisão do grupo. |
| **Épico** | Issue que reúne várias entregas. O nosso é o `#1`. |
| **Assignee** | Pessoa responsável por executar a issue. |
| **Branch** | Uma linha de trabalho com as suas alterações. |
| **main** | Branch principal, onde ficam as alterações integradas do grupo. |
| **Commit** | Registro de um conjunto de alterações na branch. |
| **Push** | Envio dos commits do computador para o GitHub. |
| **Pull Request / PR** | Pedido para revisar e juntar a sua branch à `main`. |
| **Reviewer** | Pessoa escolhida para revisar o PR. |
| **Merge** | Integração das alterações de uma branch em outra. |

## 2. Divisão combinada

| Integrante | Trabalho combinado |
|---|---|
| Samuel | 4 tarefas + gravar o vídeo. |
| Outro integrante | 4 tarefas. |
| Outro integrante | 2 tarefas + editar o vídeo. |

Antes de distribuir, ajustem a contagem: o board foi criado com **10 issues no total**, mas uma é o épico `#1`. Portanto, há **9 tarefas de execução**, as issues `#2` a `#10`. O combinado 4 + 4 + 2 prevê **10 tarefas de execução**, além de gravação e edição. Falta desdobrar uma entrega em duas tarefas e registrar as duas atividades de vídeo separadamente. Escolham esse desdobramento e os responsáveis em grupo; este guia não altera a distribuição.

As tarefas têm tamanhos diferentes. Confiram o esforço e as dependências, além da quantidade. Por exemplo, lançar consumo (`#6`) depende do catálogo (`#3`); o check-out (`#7`) utiliza os consumos.

Cada pessoa assume as tarefas combinadas em **Assignees**. Assumir quatro tarefas não significa iniciar as quatro ao mesmo tempo: mova para Em andamento somente o que começou.

## 3. Como assumir uma tarefa — dar assign

1. Abra o board e clique no título do cartão escolhido. Se abrir um painel, clique no link da issue para abrir sua página completa.
2. Leia a descrição, os critérios de aceitação e as dependências.
3. Na lateral direita, localize **Assignees**.
4. Clique na engrenagem ou no campo e selecione seu usuário. Se aparecer **assign yourself**, pode clicar nessa opção.
5. Confira se seu nome/avatar apareceu. Se houver um responsável, combinem antes de trocar.

**Assignee indica quem faz. Status indica em que etapa está.** Uma alteração não substitui a outra. Para localizar suas tarefas na lista de Issues, use o filtro `is:issue is:open assignee:@me`.

Se você não conseguir alterar o responsável, peça ao Samuel para verificar sua permissão no repositório.

## 4. Como mover o cartão

No [board](https://github.com/users/SamuelNahas/projects/3), arraste o cartão para a coluna desejada. Também é possível abrir o cartão e alterar seu campo **Status**.

| Coluna | Quando usar |
|---|---|
| **A fazer** | Tarefa escolhida ou disponível, mas ainda não iniciada. |
| **Em andamento** | Você começou a trabalhar. Continua aqui durante revisão e correções do PR. |
| **Concluído** | Critérios atendidos, validação feita e, quando há alteração no repositório, PR integrado. |

Mover o cartão atualiza o status no projeto. Fechar uma issue e mover seu cartão são ações diferentes; não dependam de uma automação sem conferir o resultado.

Se estiver bloqueado, deixe um comentário na issue, por exemplo: “Dependo da #3 para testar a seleção de itens”. Ter responsável ou abrir PR ainda não significa concluir.

## 5. Preparar o computador — uma vez

O caminho abaixo usa o [GitHub Desktop](https://desktop.github.com/), disponível para Windows e macOS. Quem já usa Git pela IDE pode seguir o mesmo fluxo na própria ferramenta.

1. Instale e abra o GitHub Desktop. Entre na sua conta do GitHub.
2. Aceite o convite de colaborador do HospedaBem, se ainda estiver pendente.
3. No Desktop, vá em **File → Clone repository → URL**.
4. Informe `https://github.com/SamuelNahas/hospedabem.git`.
5. Escolha uma pasta no computador e clique em **Clone**.
6. Abra essa pasta na IDE. Siga o [README](../README.md) para preparar Java, Maven e o banco.

Clonar cria uma cópia do projeto no computador. Salvar um arquivo na IDE ainda não envia a alteração ao GitHub.

## 6. Criar uma branch nova para a tarefa

Exemplo: você assumiu a [issue #4 — Gerenciar hóspedes](https://github.com/SamuelNahas/hospedabem/issues/4).

No GitHub Desktop:

1. Confira **Current Repository: hospedabem**.
2. Em **Current Branch**, selecione `main`.
3. Clique em **Fetch origin** para consultar as novidades. Se aparecer **Pull origin**, clique para baixar as alterações.
4. Com a `main` atualizada, abra **Current Branch → New Branch**.
5. Dê um nome como `feat/4-gerenciar-hospedes` e crie a branch a partir de `main`.
6. Clique em **Publish branch** quando quiser disponibilizá-la no GitHub.
7. Antes de editar, confira se **Current Branch** mostra a sua branch.

Se houver alterações de outra tarefa ainda sem commit, resolva-as antes de trocar de branch; não as leve para uma nova tarefa por acidente.

Padrão sugerido para o grupo:

| Tipo de trabalho | Exemplo de branch |
|---|---|
| Funcionalidade | `feat/4-gerenciar-hospedes` |
| Correção | `fix/7-total-checkout` |
| Documentação | `docs/tutorial-github` |

Use nomes curtos, sem espaços e sem acentos. Comece uma branch para cada tarefa. Termine a tarefa pelo PR antes de reutilizar seu trabalho em outra entrega.

**Se criar pelo site:** na issue, procure **Development → Create a branch**, informe o nome e use `main` como origem. Depois, no Desktop, clique em **Fetch origin** e selecione essa branch em **Current Branch**. Criar a branch pelo site não muda automaticamente a branch aberta no seu computador.

## 7. Trabalhar, fazer commit e enviar

1. Na IDE, implemente a tarefa e salve os arquivos.
2. Execute a aplicação e os testes adequados à mudança. No README estão os comandos e os requisitos de banco e interface gráfica.
3. Volte ao GitHub Desktop. Em **Changes**, confira os arquivos e as linhas alteradas.
4. Selecione somente os arquivos dessa tarefa. Não inclua arquivos pessoais ou gerados pelo ambiente.
5. Em **Summary**, escreva o que mudou. Exemplo: `feat: adiciona cadastro de hóspedes (#4)`.
6. Clique em **Commit to feat/4-gerenciar-hospedes**. Confira o nome: deve ser sua branch.
7. Clique em **Push origin**. Se ela ainda não foi publicada, use **Publish branch**.

Você pode fazer vários commits na mesma tarefa. O commit registra a alteração no computador; o push envia para o GitHub. Ambos são necessários para o grupo ver o trabalho.

## 8. Abrir um Pull Request

Depois do push:

1. Abra o [repositório](https://github.com/SamuelNahas/hospedabem).
2. Clique em **Compare & pull request**, se aparecer o aviso da sua branch. Caso não apareça, vá em **Pull requests → New pull request**.
3. Confira **base: main** e **compare: feat/4-gerenciar-hospedes**. `base` é o destino; `compare` é a branch com seu trabalho.
4. Escreva um título claro, por exemplo: `feat: implementa gerenciamento de hóspedes`.
5. Preencha a descrição com o template abaixo.
6. Em **Assignees**, marque você. Em **Reviewers**, selecione outro integrante para revisar.
7. Confira a lista de arquivos para garantir que o PR contém apenas a tarefa pretendida.
8. Clique em **Create pull request** quando estiver pronto para revisão. Se ainda faltar trabalho e a opção estiver disponível, use **Create draft pull request**.
9. Confira se o PR apareceu na lista e se a issue está vinculada. Mantenha o cartão em **Em andamento**.

### Qual template usar no PR?

Use o padrão da Lenovo-Partners, com **Descrição**, **Issue relacionada** e **Como validar**. O arquivo preparado para o HospedaBem é [.github/PULL_REQUEST_TEMPLATE.md](../.github/PULL_REQUEST_TEMPLATE.md). Depois que ele estiver publicado na branch principal, será oferecido automaticamente nos novos PRs. Se a descrição vier vazia, copie o exemplo:

```markdown
## 📝 Descrição

Implementa o cadastro, a consulta e a alteração de hóspedes para uso
nas reservas e no check-in.

## 🔗 Issue relacionada

Closes #4

## ✅ Como validar

- [ ] Entrar como recepcionista e cadastrar um hóspede.
- [ ] Buscar o hóspede e alterar seu telefone.
- [ ] Tentar cadastrar a mesma identificação e conferir a mensagem.
- [ ] Usar o hóspede em uma reserva e no check-in.
```

Troque os exemplos pelo que seu PR realmente entrega. As caixas de **Como validar** são um roteiro para quem revisar; marque somente as verificações realizadas e informe os comandos e resultados de testes que você executou.

`Closes #4` vincula a entrega e fecha a issue quando o PR for integrado à branch principal. Use isso apenas se o PR concluir a issue inteira. Para uma parte da entrega, escreva `Relacionado à #4` e mantenha a issue aberta. Não use `Closes #1` em cada PR: `#1` é o épico da iteração inteira.

## 9. Receber revisão, corrigir e concluir

1. Quem revisa abre **Files changed**, lê as alterações e executa o roteiro de validação. Dúvidas e pedidos de ajuste ficam no PR.
2. Se pedirem correções, volte à **mesma branch**, altere, teste, faça commit e push. O PR já aberto recebe esses commits; não precisa abrir outro.
3. Quando estiver pronto, avise o revisor. Se era Draft, use **Ready for review**.
4. Após a revisão e as verificações aplicáveis, quem tiver permissão e estiver combinado com o grupo faz o **merge**.
5. Confira se a issue foi fechada pelo vínculo `Closes`. Caso a entrega não use PR, como o vídeo, feche a issue somente após conferir seus critérios.
6. Confira o board e mova para **Concluído**, se necessário.
7. No Desktop, volte para `main`, use **Fetch origin** e, se aparecer, **Pull origin**, antes de criar a branch da próxima tarefa.

Se `main` avançar durante seu trabalho, salve seus commits, use **Fetch origin** e atualize sua branch com a opção de merge de `origin/main` em **Branch → Merge into current branch**, quando disponível. Teste novamente e envie o resultado. Se a ferramenta mostrar conflito e você não souber resolver, peça ajuda ao grupo antes de escolher quais alterações manter.

## 10. Criar uma task nova, inclusive para o vídeo

O tutorial inclui o template [Task](../.github/ISSUE_TEMPLATE/tarefa.md). Depois que ele estiver publicado em `main`:

1. Vá em **Issues → New issue**.
2. Escolha **Task**, para trabalho técnico, documentação, gravação ou edição. As histórias funcionais da iteração já estão criadas.
3. Escreva um título objetivo e preencha **Objetivo**. Acrescente uma lista curta de critérios para saber quando terminou.
4. Em **Assignees**, selecione a pessoa combinada; em **Milestone**, escolha **Iteração 2**; em **Projects**, escolha **HospedaBem — Iteração 2**, quando a opção estiver disponível.
5. Clique em **Create** e confira se o cartão apareceu no board em **A fazer**. Se não apareceu, use **Add item** no board para procurar e adicionar a issue existente.

Se o seletor de templates ainda não aparecer, crie uma issue em branco, copie o formato abaixo e aplique a label `task`.

Exemplo para gravação:

```markdown
## 🎯 Objetivo

Gravar a demonstração da segunda iteração do HospedaBem para a entrega.

## ✅ Critérios de Aceitação

- [ ] Combinar o roteiro com o grupo.
- [ ] Demonstrar as funcionalidades implementadas.
- [ ] Conferir se imagem e áudio estão compreensíveis.
- [ ] Disponibilizar o material bruto para quem vai editar.
```

Crie **uma issue para gravar** e **outra para editar**. A de edição deve mencionar que depende da gravação e incluir a conferência do vídeo final. Registrem nessas issues o link do material no local de compartilhamento combinado.

O épico usa **Objetivo + Escopo**; a história de usuário usa **Como / quero / para + Critérios de Aceitação**; a Task usa **Objetivo**, com critérios quando ajudarem a verificar a entrega. O template do PR é outro: ele descreve a alteração enviada para revisão.

## 11. Se algo não aparecer

| Situação | O que conferir |
|---|---|
| Board não abre | Conta correta e acesso ao projeto privado. |
| Não consigo dar assign ou mover | Permissão no repositório ou no projeto, respectivamente. |
| Meu código não aparece no GitHub | Se fez commit, se fez push e qual branch está aberta no site. |
| Minha branch não aparece no PR | Se publicou a branch e está no repositório correto. |
| Aparece “There isn't anything to compare” | Se enviou commits e se `base` e `compare` são branches diferentes. |
| Não aparece template | Se o arquivo já foi integrado à `main`; até lá, copie o exemplo deste guia. |
| A issue fechou, mas o cartão não moveu | Confira o Status no projeto e mova o cartão manualmente. |
| Pediram ajuste no PR | Corrija na mesma branch e faça novo commit e push. |

## 12. Cola para o dia a dia

**Assumir a issue → mover para Em andamento → atualizar main → criar branch → trabalhar e testar → commit → push → abrir PR → pedir revisão → corrigir → merge → conferir issue fechada e cartão Concluído.**

Para atividades de vídeo: **assumir a issue → Em andamento → produzir e compartilhar o material → conferir os critérios → fechar a issue → Concluído.**

## Referências oficiais

- [Atribuir responsáveis a issues e PRs](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/assigning-issues-and-pull-requests-to-other-github-users).
- [Organizar e mover cartões no board](https://docs.github.com/en/issues/planning-and-tracking-with-projects/customizing-views-in-your-project/customizing-the-board-layout).
- [Clonar com GitHub Desktop](https://docs.github.com/en/desktop/adding-and-cloning-repositories/cloning-and-forking-repositories-from-github-desktop).
- [Criar branches no GitHub Desktop](https://docs.github.com/en/desktop/making-changes-in-a-branch/managing-branches-in-github-desktop).
- [Fazer commits no GitHub Desktop](https://docs.github.com/en/desktop/making-changes-in-a-branch/committing-and-reviewing-changes-to-your-project-in-github-desktop).
- [Abrir um Pull Request](https://docs.github.com/en/pull-requests/how-tos/create-pull-requests/creating-a-pull-request).
- [Configurar template de PR](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/creating-a-pull-request-template-for-your-repository).
