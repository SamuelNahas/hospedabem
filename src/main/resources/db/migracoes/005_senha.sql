-- Iteração 2, RNF-8 (renovação de senha). Idempotente; não apaga dados.
-- Senha definida pelo administrador (cadastro ou redefinição) é provisória e deve ser
-- trocada pelo próprio funcionário no primeiro acesso.
ALTER TABLE funcionario ADD COLUMN IF NOT EXISTS senha_provisoria BOOLEAN NOT NULL DEFAULT FALSE;
