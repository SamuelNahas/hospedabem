# HospedaBem — Iteração 1 (Grupo N)

Felipe de Souza Rosa, Ruan Sartório Thomaz e Samuel Vieira da Cunha Nahas.

Este ZIP contém só a pasta `src/`. O projeto completo, com `pom.xml`, `README.md` e `docker-compose.yml`, está no repositório do grupo. Para reconstruir o build apenas com este material, siga os passos abaixo.

## Requisitos

- **Java 17 ou superior** (JDK, com `javac`).
- **Maven 3.9 ou superior**.
- **PostgreSQL 12 ou superior**, com o usuário `hospedabem`, o banco de uso `hospedabem` e o banco de testes `hospedabem_test`:

  ```sql
  CREATE USER hospedabem WITH PASSWORD 'hospedabem';
  CREATE DATABASE hospedabem OWNER hospedabem;
  CREATE DATABASE hospedabem_test OWNER hospedabem;
  ```

## Onde está cada coisa

- Ponto de entrada: `src/main/java/hospedabem/App.java` (classe `hospedabem.App`).
- SQL de criação e carga fictícia: `src/main/resources/db/schema.sql`, `db/seed.sql` (carga básica, usada também pelos testes) e `db/demo.sql` (carga extra de demonstração, só com `--hb.demo=true`). A aplicação os executa sozinha na primeira vez, se o banco estiver vazio.
- Configuração: `src/main/resources/application.properties`. Credenciais por variável de ambiente: `HB_DB_URL`, `HB_DB_USER` e `HB_DB_PASSWORD`. Testes: `HB_TEST_DB_URL`, `HB_TEST_DB_USER` e `HB_TEST_DB_PASSWORD`.
- Testes: `src/test/java`. Unitários (`*Test`) não usam banco. Os de integração (`*IT`) e os de ponta a ponta (`TelasE2E`) usam o banco `hospedabem_test` e recriam o esquema dele.

## Reconstruir o build

1. Crie uma pasta, coloque nela a pasta `src/` deste ZIP e salve ao lado o arquivo `pom.xml` abaixo.
2. Rode:

```bash
mvn test                        # 102 testes: unitários, integração e E2E (exige o banco hospedabem_test)
mvn package -DskipTests         # gera target/hospedabem.jar
```

## Executar com dados de demonstração (recomendado para avaliar)

Com o banco `hospedabem` vazio, rode **este comando**:

```bash
java -jar target/hospedabem.jar --hb.demo=true
```

Na primeira execução, a aplicação cria as tabelas, a carga básica (`db/seed.sql`) e a carga de demonstração (`db/demo.sql`). Nas execuções seguintes, basta `java -jar target/hospedabem.jar`: os dados ficam gravados e nunca são apagados pela aplicação.

Se o banco já tiver dados e você quiser recomeçar do zero, apague o esquema do banco de uso (nunca o de testes) e rode o comando acima de novo:

```bash
psql -U hospedabem -d hospedabem -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

Credenciais fictícias: `admin01` / `Admin@123` (administrador) e `recep01` / `Recep@123` (recepcionista).

O que a carga de demonstração contém (todos os dados são fictícios):

| Dado | Conteúdo |
|---|---|
| Hóspedes | 11 hóspedes, entre eles Carlos `11111111111`, Beatriz `22222222222`, Elisa `44444444444` e John (passaporte `XP1234567`) |
| Tipos de quarto | STD (2 pessoas, R$ 180), LUX (3, R$ 320), SUI (4, R$ 550) e EXE (2, R$ 260) |
| Quartos | 101, 102, 103 (STD; 103 em manutenção), 201, 202 (LUX), 301, 302 (SUI), 501, 502, 503 (EXE; 503 em manutenção) |
| Reservas | **1001** (Carlos, LUX, de hoje a hoje+3: use para o check-in com reserva), futuras confirmadas (2001 a 2004), uma cancelada (2005) e uma já convertida em hospedagem (2006) |
| Hospedagens ativas | 9001 no quarto 501 e 9002 no quarto 301, com conta aberta |

Roteiro curto para testar cada caso de uso:

1. **Login (UC01):** entre como `recep01`; os cadastros aparecem desabilitados. Saia e entre como `admin01`.
2. **Disponibilidade (UC07):** de hoje a hoje+3, todos os tipos. O EXE fica sem vaga amanhã (hospedagem 9001 e reserva 2001).
3. **Reserva (UC08):** Beatriz `22222222222`, STD, de amanhã a amanhã+3, 2 hóspedes. Com 3 hóspedes, a capacidade é recusada.
4. **Check-in com reserva (UC10):** reserva `1001`, hóspedes `11111111111` e `22222222222`, quarto 201.
5. **Check-in sem reserva (UC10):** responsável `XP1234567`, STD, de hoje a hoje+2, hóspedes `XP1234567` e `44444444444`, quarto 101.
6. **Cadastros (UC03 e UC04):** inclua um tipo e um quarto. A situação OCUPADO é recusada no cadastro.

### pom.xml

Dependências: Spring Boot 3.4.1 (`spring-boot-starter-jdbc`, sem servidor web), JavaFX 21.0.5 (`org.openjfx:javafx-controls`; o Maven baixa os nativos do sistema em que o build é feito), driver `org.postgresql:postgresql` (versão gerenciada pelo Spring Boot), `spring-boot-starter-test` (JUnit 5) e o PIT 1.17.4 para os testes de mutação (`mvn test-compile org.pitest:pitest-maven:mutationCoverage`).

Os testes E2E (`TelasE2E`) abrem janelas JavaFX: rode-os numa sessão gráfica (em Linux sem monitor, `xvfb-run mvn test`).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.1</version>
    <relativePath/>
  </parent>

  <groupId>br.ufms.grupon</groupId>
  <artifactId>hospedabem</artifactId>
  <version>1.0.0-iteracao1</version>
  <name>HospedaBem - Iteração 1 (Grupo N)</name>

  <properties>
    <java.version>17</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <!-- Sem starter web: aplicação desktop JavaFX. JDBC traz JdbcTemplate, HikariCP e @Transactional. -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <!-- Interface desktop: JavaFX (LTS 21, compatível com Java 17). O Maven baixa os nativos da plataforma do build. -->
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
      <version>21.0.5</version>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <!-- Exigido pelo plugin JUnit 5 do PIT (testes de mutação). -->
    <dependency>
      <groupId>org.junit.platform</groupId>
      <artifactId>junit-platform-launcher</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <finalName>hospedabem</finalName>
    <plugins>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <includes>
            <include>**/*Test.java</include>
            <include>**/*IT.java</include>
            <include>**/*E2E.java</include>
          </includes>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
      <!-- Testes de mutação: mvn test-compile org.pitest:pitest-maven:mutationCoverage -->
      <plugin>
        <groupId>org.pitest</groupId>
        <artifactId>pitest-maven</artifactId>
        <version>1.17.4</version>
        <dependencies>
          <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.1</version>
          </dependency>
        </dependencies>
        <configuration>
          <!-- Regras de negócio e coordenação; telas são cobertas pelos testes E2E. -->
          <targetClasses>
            <param>hospedabem.model.*</param>
            <param>hospedabem.controller.*</param>
            <param>hospedabem.service.*</param>
          </targetClasses>
          <targetTests>
            <param>hospedabem.*</param>
          </targetTests>
          <!-- Utilitário de linha de comando para gerar hashes da carga inicial. -->
          <excludedMethods>
            <param>main</param>
          </excludedMethods>
          <excludedTestClasses>
            <param>hospedabem.view.*E2E</param>
          </excludedTestClasses>
          <!-- Os testes de integração recriam o mesmo banco de teste: uma thread evita interferência. -->
          <threads>1</threads>
          <timeoutConstant>15000</timeoutConstant>
          <!-- Medido: 97%. Os sobreviventes são equivalentes documentados no README. -->
          <mutationThreshold>95</mutationThreshold>
          <outputFormats>
            <param>HTML</param>
            <param>XML</param>
          </outputFormats>
          <timestampedReports>false</timestampedReports>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```
