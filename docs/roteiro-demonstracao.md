# Roteiro do vídeo (até 10 minutos)

Antes de gravar: `docker compose up -d`. Para começar com a carga limpa, recrie o banco de uso:
`docker exec hospedabem-pg psql -U hospedabem -d hospedabem -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"`.
**Atenção:** esse comando apaga os dados do banco de uso. Depois, `mvn package -DskipTests`.

| Tempo | O que mostrar |
|---|---|
| 0:00–1:30 | **Organização e banco.** Árvore `model/ dao/ controller/ view/`. `schema.sql`: índice único de hospedagem ativa por quarto, `UNIQUE` da conta e da reserva convertida. Regras no `model` (`Atendimento.criarHospedagem`, `Quarto`, `Reserva`) e `ControladorCheckIn.confirmarCheckIn()` com `@Transactional` e bloqueios. `Ocupacao.pico`. Rodar `mvn test` e mostrar *100 testes, 0 falhas* (unitários, integração e E2E). Abrir `target/pit-reports/index.html` (mutação, 97%). |
| 1:30–2:30 | **Login e permissões.** `java -jar target/hospedabem.jar`. Senha errada → mensagem. Entrar como `recep01`: *Tipos de quarto* e *Quartos* desabilitados na barra lateral. Mencionar `AutenticacaoIT.recepcionistaNaoGerenciaTiposNemQuartosMesmoChamandoOControlador`. Sair. |
| 2:30–3:30 | **Tipos de quarto** (`admin01`). Incluir `FAM`, capacidade 5, R$ 450,00. Tentar código `STD` repetido. Tentar reduzir LUX para capacidade 1 (recusado: a reserva 1001 tem 2 hóspedes). |
| 3:30–4:30 | **Quartos.** Incluir 401 (FAM). Tentar situação OCUPADO pelo cadastro (recusado). Tentar excluir o tipo FAM (recusado: tem quarto). |
| 4:30–5:30 | **Disponibilidade.** Hoje → hoje+3, todos os tipos: LUX com ocupação 1 (reserva 1001), STD com 2 operacionais (103 em manutenção). Mostrar o período inválido. |
| 5:30–6:30 | **Reserva.** CPF `22222222222`, STD, amanhã → +3, 3 hóspedes (recusado pela capacidade). Com 2 hóspedes: *Verificar disponibilidade* e *Confirmar reserva*, com o código gerado. |
| 6:30–8:15 | **Check-in com reserva 1001.** Iniciar. Adicionar `11111111111` (e repeti-lo, para mostrar a recusa) e `22222222222`. Consultar quartos, selecionar 201, ver o resumo e confirmar: hospedagem, entrada, quarto e **conta aberta**. Tentar iniciar 1001 de novo (recusado: convertida). |
| 8:15–9:15 | **Check-in sem reserva.** `XP1234567`, STD, hoje → +2. Adicionar `XP1234567` e `44444444444`. Um terceiro hóspede é recusado pela capacidade. Selecionar quarto e confirmar. O aviso verde mostra a hospedagem, o quarto e a conta aberta. |
| 9:15–10:00 | **Conta aberta e persistência.** Fechar e reabrir a aplicação. Consulta: `docker exec hospedabem-pg psql -U hospedabem -d hospedabem -c "SELECT h.codigo, q.numero, q.situacao, c.id AS conta, c.situacao FROM hospedagem h JOIN quarto q ON q.id=h.quarto_id JOIN conta_hospedagem c ON c.hospedagem_id=h.id;"` |
