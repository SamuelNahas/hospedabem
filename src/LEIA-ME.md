# HospedaBem — Iteração 2 (Grupo N)

Felipe de Souza Rosa, Ruan Sartório Thomaz e Samuel Vieira da Cunha Nahas.

O ZIP contém somente `src/`. Dentro dela, `entrega/` guarda cópias do `pom.xml`, da configuração Checkstyle e da documentação desta versão, para reconstruir o projeto sem depender do repositório. UC01–UC12 estão implementados, incluindo os seis novos UCs (UC02, UC05, UC06, UC09, UC11 e UC12) e a renovação de senha RNF-8. Comprovantes e relatórios UC13–UC18 ficam para a iteração 3.

## Preparar o projeto extraído

Requisitos: JDK 17 ou superior (com `javac`), Maven 3.9 ou superior e PostgreSQL 12 ou superior (a validação usa PostgreSQL 17). JavaFX abre janelas; os E2E precisam de sessão gráfica. Em Linux sem monitor, use `xvfb-run --auto-servernum` antes do comando Maven dos E2E. Compile no sistema operacional de destino para que o Maven inclua os componentes nativos corretos do JavaFX.

1. Extraia o ZIP em uma pasta nova e entre nela. Ela deve conter `src/`.
2. Reconstrua os arquivos auxiliares:

   ```bash
   cp src/entrega/pom.xml pom.xml
   cp -R src/entrega/config config
   ```

3. Crie o usuário e os bancos no PostgreSQL, como administrador do banco:

   ```sql
   CREATE USER hospedabem WITH PASSWORD 'hospedabem';
   CREATE DATABASE hospedabem OWNER hospedabem;
   CREATE DATABASE hospedabem_test OWNER hospedabem;
   ```

   Essas credenciais são fictícias para avaliação local. Configure o acesso real por variáveis de ambiente.

4. Compile:

   ```bash
   mvn --batch-mode -DskipTests package
   ```

   O JAR fica em `target/hospedabem.jar`. Checkstyle roda automaticamente na fase `validate`; a cópia de `config/` acima é necessária.

## Configurar e executar

| Variável | Padrão |
|---|---|
| `HB_DB_URL` | `jdbc:postgresql://localhost:5432/hospedabem` |
| `HB_DB_USER` | `hospedabem` |
| `HB_DB_PASSWORD` | `hospedabem` |
| `HB_TEST_DB_URL` | `jdbc:postgresql://localhost:5432/hospedabem_test` |
| `HB_TEST_DB_USER`, `HB_TEST_DB_PASSWORD` | `hospedabem` |

Na primeira abertura, a aplicação cria o esquema/carga básica somente se o banco estiver vazio e aplica as migrações da iteração 2. Para demonstrar, use um banco vazio separado do banco real:

```bash
java -jar target/hospedabem.jar --hb.demo=true
```

Nas demais aberturas, use `java -jar target/hospedabem.jar`. Os dados persistem. A opção de demonstração só carrega dados na criação do banco; não repete nem recarrega a carga num banco já populado.

Credenciais fictícias: `admin01` / `Admin@123` (administrador) e `recep01` / `Recep@123` (recepcionista). Senhas com 90 dias ou mais precisam ser renovadas; senhas definidas pelo administrador são provisórias e exigem troca no primeiro acesso. A política escolhida pela equipe exige ao menos 8 caracteres sem contar espaços, com letra e número, confirmação e senha diferente da atual.

## Atualizar o banco da iteração 1

Não apague o esquema e não reaplique `schema.sql`, `seed.sql` ou `demo.sql` sobre dados existentes.

1. Pare as estações e faça backup: `pg_dump -Fc -h SERVIDOR -U hospedabem -d hospedabem -f hospedabem-antes-iteracao2.dump`.
2. Gere o novo JAR e configure o mesmo banco de uso por `HB_DB_URL`, usuário e senha. O usuário precisa ter permissão de alterar o esquema.
3. Abra uma estação. O `InicializadorBanco` aplica as migrações pendentes em transação e preserva dados antigos. Confira login, reservas e hospedagens antes de abrir as demais estações.
4. Avise a recepção: funcionários com senha vencida precisarão trocá-la no primeiro acesso.

`MigracaoIteracaoUmIT` cobre a preservação das linhas, a estrutura equivalente à de instalação nova e operações sobre dados antigos. Backup/restauração e reversão de versão não foram validados nesta entrega.

## Demonstrar os seis novos casos de uso

O roteiro completo e os valores esperados ficam em [entrega/docs/iteracao-2/demonstracao.md](entrega/docs/iteracao-2/demonstracao.md).

- **UC02:** como administrador, cadastre um funcionário; no primeiro login dele, troque a senha provisória.
- **UC05:** como administrador, gerencie os itens de consumo e serviço. Preço positivo, até duas casas decimais.
- **UC06:** nos dois perfis, cadastre um hóspede com CPF válido ou passaporte, nome, nascimento e telefone. Cadastros com histórico não são excluídos.
- **UC09:** localize a reserva pelo código, altere dados de uma reserva confirmada ou confirme o cancelamento. Canceladas e convertidas não são alteráveis.
- **UC11:** selecione uma hospedagem ativa com conta aberta e lance um item/quantidade. O preço é relido no catálogo na gravação e preservado no lançamento.
- **UC12:** localize a hospedagem, confira diárias + consumos, escolha dinheiro/pix/cartão de crédito/débito e confirme. Recusar o diálogo mantém a conta aberta e o check-out pendente; confirmar registra pagamento, fecha conta, encerra hospedagem e libera o quarto numa transação.

A carga inclui reservas confirmadas/canceladas/convertidas; a hospedagem **9001** está ativa com consumo e pagamento pendente, a **9002** está ativa e a **9003** tem check-out concluído com pagamento PIX e quarto liberado. Consulte o roteiro para valores e datas relativas ao dia da criação do banco.

Funcionários, tipos de quarto, quartos e catálogo são exclusivos do administrador. Hóspedes, disponibilidade, reserva, alteração/cancelamento, check-in, consumo, check-out e troca da própria senha estão disponíveis aos dois perfis. Sem sessão válida ou com senha vencida, as operações são recusadas.

As diárias seguem uma decisão da equipe: noites de calendário usadas, mínimo uma diária, sem multa por atraso ou adicional por horário. A diária é preservada no check-in; a reserva não garante tarifa. O catálogo alterado não recalcula consumos antigos. A tela de conta é um resumo para pagamento, sem impressão de comprovantes/relatórios da iteração 3.

## Executar a validação

Os testes de integração/E2E e a mutação recriam somente o banco `hospedabem_test`. Nunca aponte as variáveis `HB_TEST_*` para dados reais e não rode integrações/mutação simultaneamente contra o mesmo banco.

```bash
mvn --batch-mode checkstyle:check
mvn --batch-mode clean test -Dtest='*Test'
mvn --batch-mode test -Dtest='*IT'
mvn --batch-mode test -Dtest='*E2E' -Dhb.e2e.capturas=target/e2e-capturas
mvn --batch-mode -DskipTests package
mvn --batch-mode test-compile org.pitest:pitest-maven:mutationCoverage
```

A rodada da #9 registrou 246 testes aprovados e 96% PIT. Comandos, ambiente e limitações daquela execução estão em [entrega/docs/validacao-iteracao-2.md](entrega/docs/validacao-iteracao-2.md). Esses números são históricos, não comprovam uma execução ao extrair este pacote. O PIT exige no mínimo 95%.

## Mapa dos arquivos

- Entrada: `main/java/hospedabem/App.java` (classe `hospedabem.App`).
- Recursos/configuração: `main/resources/application.properties`, `main/resources/db/{schema,seed,demo}.sql` e `main/resources/db/migracoes/`.
- Testes: `test/java/hospedabem/`; fixture histórica da iteração 1 em `test/resources/iteracao1/`.
- Build reproduzido: `entrega/pom.xml`, `entrega/config/checkstyle.xml`.
- README, matriz RF/RNF × UC × telas × testes e artefatos: `entrega/README.md`, `entrega/docs/`.

Os caminhos deste mapa são relativos à pasta `src/`; os comandos Maven são executados na pasta acima dela. O repositório é [SamuelNahas/hospedabem](https://github.com/SamuelNahas/hospedabem).
