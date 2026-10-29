# Demonstração das telas reais (29/10/2026)

## Como foi executada

1. Aplicação empacotada e executada com `java -jar target/hospedabem.jar` contra o banco `hospedabem`, inicialmente vazio. O log registrou `Banco vazio: esquema e carga fictícia criados.`
2. O macOS desta máquina nega ao terminal a permissão de gravação de tela e de acessibilidade. Por isso, não foi possível clicar nas janelas "de fora". As telas foram conduzidas por um roteiro Java que roda na mesma JVM da aplicação. Ele abre as telas Swing **reais** (`TelaLogin` → `TelaPrincipal` → ...), preenche os campos, aciona os botões com `doClick()` e renderiza cada janela em PNG. As regras, os controladores e o banco são os mesmos da aplicação normal.
3. Essa execução encontrou um erro real: a tela de check-in acionava a carga dos tipos antes de estar dentro de uma janela (`NullPointerException` em `Tarefa`). O erro foi corrigido e a demonstração foi repetida do zero.

**Limitação:** isso não substitui o vídeo com interação humana. Para gravá-lo, siga [roteiro-demonstracao.md](roteiro-demonstracao.md).

## Mensagens exibidas pelo sistema

| Passo | Mensagem |
|---|---|
| Login com senha errada | Matrícula ou senha inválida. |
| Inclusão do tipo FAM | Tipo FAM gravado. |
| Tipo com código STD repetido | Já existe tipo de quarto com o código STD. |
| Inclusão do quarto 401 | Quarto 401 gravado. |
| Quarto com situação OCUPADO pelo cadastro | Situação OCUPADO não pode ser definida pelo cadastro: ela resulta do check-in ou da reserva. Use DISPONIVEL ou MANUTENCAO. |
| Disponibilidade com saída = entrada | Período inválido: a saída deve ser posterior à entrada. |
| Reserva STD com 3 hóspedes | Quantidade de hóspedes (3) excede a capacidade do tipo STD (2). |
| Reserva válida (administrador) | Reserva 1002 registrada. |
| Check-in 1001: hóspede repetido | Hóspede já informado: Carlos Teste Silva. |
| Check-in 1001: CPF inexistente | Hóspede não cadastrado: 99999999999. O cadastro de hóspedes (UC06) será entregue na iteração 2; regularize o cadastro e reinicie o check-in. |
| Check-in 1001 confirmado | Check-in realizado com sucesso (hospedagem 5001, quarto 201, conta nº 1 ABERTA). |
| Check-in sem reserva: 3º hóspede em STD | Capacidade do quarto excedida: o tipo STD comporta 2 hóspede(s). |
| Check-in sem reserva confirmado | Check-in realizado com sucesso (hospedagem 5002, quarto 101, conta nº 2 ABERTA). |

## Estado do banco após a demonstração

```
{hospedagem=5001, quarto=201, situacao_quarto=OCUPADO, com_reserva=true,  conta=1, situacao_conta=ABERTA, valor_diaria=320.00, hospedes=2}
{hospedagem=5002, quarto=101, situacao_quarto=OCUPADO, com_reserva=false, conta=2, situacao_conta=ABERTA, valor_diaria=180.00, hospedes=2}
{codigo=1001, situacao=CONVERTIDA, quantidade_hospedes=2, funcionario_id=2}
{codigo=1002, situacao=CONFIRMADA, quantidade_hospedes=2, funcionario_id=1}
```

A reserva 1001 foi registrada pela recepcionista na carga inicial (funcionário 2). A reserva 1002 foi registrada pelo administrador logado (funcionário 1).

## Capturas

As capturas estão em [telas/](telas/), na ordem do roteiro. Por exemplo:

- [03 - menu da recepcionista com cadastros desabilitados](telas/03-menu-recepcionista-cadastros-desabilitados.png)
- [11 - disponibilidade](telas/11-disponibilidade-hoje-a-3-dias.png)
- [18 - resumo do check-in com reserva](telas/18-checkin-com-reserva-resumo.png)
- [19 - check-in confirmado com a conta aberta](telas/19-checkin-com-reserva-confirmado.png)
