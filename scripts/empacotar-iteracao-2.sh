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
cp README.md "$stage/src/entrega/README.md"
cp -R docs "$stage/src/entrega/docs"
archive="$repo/target/GrupoN_Iteracao2.zip"
rm -f "$archive"
(cd "$stage" && zip -q -r -X "$archive" src -x '*/.DS_Store')
unzip -tq "$archive"
if unzip -Z1 "$archive" | grep -v '^src/' | grep -q .; then
    echo 'Pacote inválido: entrada fora de src/' >&2
    exit 1
fi
echo "$archive"
