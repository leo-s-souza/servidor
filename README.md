# CAPP

Repositório oficial do servidor CAPP responsável por gerenciar os dados do aplicativo E-gula.

---

## Geral

A função do servidor é prover o suporte necessário ao aplicativo no que diz respeito à usuários, pedidos e imagens.

Utilizamos um banco de dados Firebird com acesso em máquina local.

As comunicações externas são feitas exclusivamente via HTTP com modelo RESTful.

Como gerenciador de dependências, usamos o Gradle, portanto é necessário que a IDE a ser utilizada tenha suporte. De preferência, usar o [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/).

**Sempre mantenha o código limpo, formatado e bem documentado.**

---

## Modelagem
 
Possuímos uma pasta denominada "modelagem" neste repositório que guarda arquivos relacionados a modelagem da base central utilizada pelo CAPP. Sempre manter essa modelagem atualizada com as ultima modificações da base.

Caso seja necessário recriar a base, pode-se usar os comandos SQL contidos nessa pasta.

---

# Licença
```
Copyright (C) 2018 Casa da Automação Ltda.
Todos os direitos reservados.
```