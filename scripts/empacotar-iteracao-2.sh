#!/bin/sh
set -eu

repo=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo"
mkdir -p target
stage=$(mktemp -d "$repo/target/entrega-iteracao2.XXXXXX")
trap 'rm -rf "$stage"' EXIT HUP INT TERM
cp -R src "$stage/src"
mkdir -p "$stage/src/entrega/config"
cp pom.xml "$stage/src/entrega/pom.xml"
cp config/checkstyle.xml "$stage/src/entrega/config/checkstyle.xml"
{
    printf '%s\n\n' '> Cópia de referência do README do repositório: os comandos abaixo pressupõem o checkout completo, incluindo `scripts/`, `.github/` e `docker-compose.yml`. Para compilar, testar e executar usando somente este ZIP, siga [LEIA-ME](../LEIA-ME.md).'
    sed -e 's|(src/LEIA-ME.md)|(../LEIA-ME.md)|g' \
        -e 's|(.github/workflows/ci.yml)|(https://github.com/SamuelNahas/hospedabem/blob/main/.github/workflows/ci.yml)|g' README.md
} > "$stage/src/entrega/README.md"
cp -R docs "$stage/src/entrega/docs"
sed 's|(../.github/|(https://github.com/SamuelNahas/hospedabem/blob/main/.github/|g' \
    docs/tutorial-github.md > "$stage/src/entrega/docs/tutorial-github.md"
archive="$repo/target/GrupoN_Iteracao2.zip"
rm -f "$archive"
(cd "$stage" && zip -q -r -X "$archive" src -x '*/.DS_Store')
unzip -tq "$archive"
if unzip -Z1 "$archive" | grep -v '^src/' | grep -q .; then
    echo 'Pacote inválido: entrada fora de src/' >&2
    exit 1
fi
echo "$archive"
