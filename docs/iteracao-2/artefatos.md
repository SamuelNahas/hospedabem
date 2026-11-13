# Artefatos da segunda iteração

Revisão de 13/11/2026, alinhada ao código de `e354fcf` e às entregas UC02, UC05, UC06, UC09, UC11, UC12 e RNF-8. Os artefatos da primeira iteração permanecem como histórico; os documentos abaixo descrevem o estado implementado da segunda.

## Exigências e fontes conferidas

| Fonte local da disciplina | Evidência e consequência |
|---|---|
| `Trabalho Prático/Definição do Trabalho Prático.pdf`, p.4 | Aplicação Java desktop com persistência em SGBD relacional. |
| Mesmo documento, pp.8–9 | Relação de entregas: visão geral, processo de negócio, requisitos, listagem, modelo conceitual, expansão, sequência/contrato, comunicação, classes de projeto; J e K são apresentações da primeira e segunda iterações. |
| `Trabalho Prático/Etapas finais do Trabalho Prático.pdf`, pp.2–8 | MVC, lógica de domínio e persistência, DAO/JDBC ou framework e interface desktop. |
| `GrupoN-APSOO/atividadeC/GrupoN_DocRequisitos_v3.docx`, revisão 2.2 | RF-2/3/5/6/8/11/12/16/19–23 e RNF-8/10/11/12 sustentam o escopo entregue. |
| `GrupoN-APSOO/atividadeD/GrupoN_ListagemCasosDeUso_.docx`, §§3–4 | Iteração 2: UC02/05/06/09/11/12; iteração 3: UC13–18. |
| `GrupoN-APSOO/atividadeE`, `atividadeF`, `atividadeG`, `AtividadeH` | Expansão, modelo conceitual, DSS/contrato e comunicação/classes existentes servem de histórico para as revisões abaixo. |

Os dois primeiros PDFs foram conferidos na pasta local `Documents/apsoo/Trabalho Prático/`. As letras das entregas no enunciado diferem das pastas E/F e H/I do material do grupo: a correspondência aqui é pelo **nome do artefato**. Não se alterou o documento original da disciplina nem se atribuiu à professora uma regra decidida pela equipe.

Os enunciados consultados não especificam nome/estrutura do ZIP, duração/formato do vídeo nem reenvio obrigatório de todos os modelos na K. O ZIP com `src/` e o MP4 da atividade J são um precedente da entrega anterior, não uma exigência comprovada da K. Conferir a tarefa K do AVA antes da submissão definitiva.

## Revisões disponíveis

| Artefato | Revisão da iteração 2 |
|---|---|
| Visão/processo/requisitos/listagem | Escopo e permissões do [README](../../README.md); RF/RNF mantêm os números dos originais. O ciclo passa a alcançar pagamento e liberação do quarto. |
| Modelo conceitual | [Conceitos, associações e invariantes](modelo-conceitual.md), incluindo catálogo, consumo e pagamento. |
| Expansão dos casos de uso | [Seis casos de uso e renovação de senha](casos-de-uso.md). |
| DSS e contratos de operação | [Operações e efeitos observáveis](operacoes.md), com confirmação, recusa e rollback. |
| Comunicação e classes de projeto | [Objetos, mensagens e responsabilidades](projeto.md), com os nomes reais do código. |
| Evidências | [Validação da #9](../validacao-iteracao-2.md): execução local registrada, limites e lacunas. |

Os diagramas estão em Mermaid dentro dos arquivos Markdown: o próprio arquivo é a fonte editável e é renderizado pelo GitHub na visualização do documento. Representam recortes do sistema; não inventariam classes de infraestrutura. `Extrato` é cálculo para conferência do UC12: impressão de comprovantes/extratos e relatórios **UC13–UC18 continua na iteração 3**.
