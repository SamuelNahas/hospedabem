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
- SQL de criação e carga fictícia: `src/main/resources/db/schema.sql` e `src/main/resources/db/seed.sql`. A aplicação os executa sozinha na primeira vez, se o banco estiver vazio.
- Configuração: `src/main/resources/application.properties`. Credenciais por variável de ambiente: `HB_DB_URL`, `HB_DB_USER` e `HB_DB_PASSWORD`. Testes: `HB_TEST_DB_URL`, `HB_TEST_DB_USER` e `HB_TEST_DB_PASSWORD`.
- Testes: `src/test/java`. Os de integração (`*IT`) usam o banco `hospedabem_test` e recriam o esquema dele.

## Reconstruir o build

1. Crie uma pasta, coloque nela a pasta `src/` deste ZIP e salve ao lado o arquivo `pom.xml` abaixo.
2. Rode:

```bash
mvn test                        # testes (exige o banco hospedabem_test)
mvn package -DskipTests         # gera target/hospedabem.jar
java -jar target/hospedabem.jar # executa a aplicação desktop
```

Credenciais fictícias de demonstração: `admin01` / `Admin@123` (administrador) e `recep01` / `Recep@123` (recepcionista).

### pom.xml

Dependências: Spring Boot 3.4.1 (`spring-boot-starter-jdbc`, sem servidor web), driver `org.postgresql:postgresql` (versão gerenciada pelo Spring Boot) e `spring-boot-starter-test` (JUnit 5) para os testes.

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
    <!-- Sem starter web: aplicação desktop Swing. JDBC traz JdbcTemplate, HikariCP e @Transactional. -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
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
          </includes>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```
