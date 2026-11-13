# Demonstração da iteração 2

Todos os nomes e documentos da carga são fictícios. Prepare um **banco novo e separado para demonstração**, configure `HB_DB_URL`, `HB_DB_USER` e `HB_DB_PASSWORD` e execute `java -jar target/hospedabem.jar --hb.demo=true`. A carga extra só entra se o banco estiver vazio, depois das migrações. Em banco existente, essa opção não acrescenta nem recarrega dados; a atualização preserva o histórico. Para repetir a demonstração, crie outro banco vazio.

Entre com `admin01` / `Admin@123` ou `recep01` / `Recep@123`. São credenciais de demonstração, não de uso real. Dados gravados em dias posteriores continuam com as datas originais; os valores abaixo pressupõem demonstrar no dia da criação da carga.

## Situações já preparadas

| Situação | Dados verificáveis |
|---|---|
| Reserva para check-in | 1001, Carlos `11111111111`, LUX, hoje a hoje+3 |
| Alteração/cancelamento | 2001–2004 confirmadas; 2005 já cancelada; 2006 convertida |
| Consumo e pagamento pendente | Hospedagem 9001, quarto 501 EXE, conta ABERTA, 2 águas de R$ 5,50 = R$ 11,00; nenhuma linha de pagamento. Conferência no dia da carga: 1 diária de R$ 260,00 + R$ 11,00 = R$ 271,00 |
| Outra hospedagem ativa | 9002, quarto 301 SUI, reserva 2006 convertida, conta aberta |
| Check-out concluído | 9003, quarto 502 EXE liberado, conta FECHADA e hospedagem ENCERRADA; 2 diárias de R$ 260,00 + 2 águas = R$ 531,00, pago em PIX |
| Catálogo | AGUA, Água mineral, R$ 5,50; LAVANDERIA, Lavagem de roupa, R$ 25,00 |

Pagamento pendente significa conta aberta sem pagamento; não há estado `PENDENTE` na tabela `pagamento`. O UC12 registra apenas pagamentos confirmados. O pagamento PIX da carga é fictício: a aplicação registra a forma informada pela recepção e não acessa um provedor financeiro.

## Roteiro para apresentação

1. Entre como administrador. Mostre funcionários (UC02), tipos/quartos (UC03/04) e catálogo (UC05); inclua um item e explique que preço alterado não recalcula consumos existentes.
2. Cadastre uma funcionária com CPF válido, perfil recepcionista e senha provisória. Saia, entre com essa conta e troque a senha no primeiro acesso (RNF-8). Mostre que funcionários, tipos/quartos e catálogo são exclusivos do administrador, enquanto hóspedes e atendimento permitem os dois perfis.
3. Cadastre hóspede com CPF válido ou passaporte (UC06). Consulte disponibilidade (UC07), registre uma reserva STD futura (UC08) e localize-a pelo código. Altere a saída e depois cancele com confirmação (UC09); confira a devolução de capacidade. A reserva 2005 demonstra a recusa de alterar uma já cancelada.
4. Faça check-in da reserva 1001 com Carlos e Beatriz `22222222222` no quarto 201 (UC10). Mostre o resumo antes de confirmar e a reserva convertida depois.
5. Em **Lançar consumo**, selecione a hospedagem 9001 e confira as duas águas já lançadas (UC11). Se lançar mais itens, o total do próximo passo muda.
6. Em **Check-out**, selecione 9001 e confira diárias e consumo. Tente confirmar sem forma de pagamento; escolha uma forma, recuse o diálogo e confira que a hospedagem continua ativa. Confirme de novo e conclua (UC12). São uma página e um diálogo de confirmação. O quarto 501 retorna à disponibilidade.
7. Mostre a hospedagem 9003 encerrada e o pagamento fictício de R$ 531,00 por consulta ao banco (a interface de UC12 lista hospedagens ativas). Reinicie e confirme que o estado se mantém. Não prometa consulta de histórico/relatórios pela tela: UC13–UC18 pertencem à iteração 3.

Consulta de apoio, somente leitura:

```sql
SELECT h.codigo, h.situacao AS hospedagem, q.numero, q.situacao AS quarto,
       c.situacao AS conta, p.forma, p.valor_total
FROM hospedagem h JOIN quarto q ON q.id = h.quarto_id
JOIN conta_hospedagem c ON c.hospedagem_id = h.id
LEFT JOIN pagamento p ON p.conta_hospedagem_id = c.id
ORDER BY h.codigo;
```

## Evidências

`DemonstracaoIT.cargaDemonstraPagamentoPendenteEConcluidoSemRecarregarNoReinicio` executa a inicialização real com `hb.demo=true`, confere cancelamento, conta pendente com consumo, check-out concluído e repetição da inicialização sem duplicação. O ciclo completo operado pelas telas é `TelasE2E.cicloCompletoDaIteracaoDoisPelasTelas`; seus resultados anteriores estão no [relatório da #9](../validacao-iteracao-2.md). O teste de demonstração verifica a carga; não substitui a apresentação pela interface.

Os enunciados consultados pedem apresentação da segunda iteração (K), mas não determinam formato ou duração de vídeo. Este roteiro prepara a apresentação; não é uma gravação entregue.
