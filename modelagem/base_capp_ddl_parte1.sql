/* Criação dos domains */

CREATE DOMAIN DM_BOOL AS SMALLINT CHECK (value is null or value in (0, 1));
CREATE DOMAIN DM_CEP AS CHAR(8) COLLATE UTF8;
CREATE DOMAIN DM_CNPJ AS CHAR(14) COLLATE UTF8;
CREATE DOMAIN DM_CPF AS CHAR(11) COLLATE UTF8;
CREATE DOMAIN DM_DESCRICAO AS VARCHAR(100) COLLATE UTF8;
CREATE DOMAIN DM_DESCRICAO_LONGA AS VARCHAR(250) COLLATE UTF8;
CREATE DOMAIN DM_DESCRICAO_LONGA_EXTRA AS VARCHAR(500) COLLATE UTF8;
CREATE DOMAIN DM_EMAIL AS VARCHAR(100) COLLATE UTF8;
CREATE DOMAIN DM_HOST_PORT AS VARCHAR(40) COLLATE UTF8;
CREATE DOMAIN DM_LOJA_STATUS AS SMALLINT DEFAULT 0 NOT NULL CHECK (value in (0, 1, 2, 3, 4, 5));
CREATE DOMAIN DM_MAC AS CHAR(12) COLLATE UTF8;
CREATE DOMAIN DM_MD5 AS CHAR(32) COLLATE UTF8;
CREATE DOMAIN DM_MODO_SERVIDOR AS SMALLINT CHECK (value in (0, 1, 2, 3));
CREATE DOMAIN DM_MONEY AS DECIMAL(18,4);
CREATE DOMAIN DM_NOME AS VARCHAR(60) COLLATE UTF8;
CREATE DOMAIN DM_NOTIFICACAO_TIPO AS SMALLINT CHECK (value in (0, 1, 2, 3));
CREATE DOMAIN DM_PORCENTAGEM AS DECIMAL(3,2);
CREATE DOMAIN DM_SOBRENOME AS VARCHAR(60) COLLATE UTF8;
CREATE DOMAIN DM_SO_DISPOSITIVO AS SMALLINT CHECK (value in (0, 1));
CREATE DOMAIN DM_TELEFONE AS VARCHAR(20) COLLATE UTF8;
CREATE DOMAIN DM_TEXTO AS blob SUB_TYPE text COLLATE UTF8;
CREATE DOMAIN DM_TIPO_API AS SMALLINT CHECK (value in (0, 1));
CREATE DOMAIN DM_TIPO_ARQUIVO_DISPONIVEL AS SMALLINT CHECK (value in (0, 1, 2));
CREATE DOMAIN DM_TIPO_ARQUIVO_LOJA AS SMALLINT CHECK (value in (0, 1, 2, 3, 4, 5));
CREATE DOMAIN DM_TIPO_USUARIO AS SMALLINT DEFAULT 0 NOT NULL CHECK (value in (0, 1));
CREATE DOMAIN DM_USO_STATUS AS SMALLINT CHECK ( value in (0, 1));
CREATE DOMAIN DM_REFERENCIA AS VARCHAR(250);
comment on domain DM_BOOL is '1 => true | 0 => false';
comment on domain DM_HOST_PORT is '/host:port (ipv4/ipv6 + port)';
comment on domain DM_LOJA_STATUS is '0 => Indisponivel | 1 => Habilitado | 2 => Modo Testes | 3 => Restrito Autorizaçao | 4 => Modo Garçom | 5 => Garçom em Testes';
comment on domain DM_MODO_SERVIDOR is '0 => Normal | 1 => Master';
comment on domain DM_NOTIFICACAO_TIPO is '0 => Bonus | 1 => Pedido | 2 => Usuario | 3 => Central';
comment on domain DM_SO_DISPOSITIVO is '0 => Android | 1 = iOS';
comment on domain DM_TIPO_API is '0 => FCM | 1 => Google Maps';
comment on domain DM_TIPO_ARQUIVO_DISPONIVEL is '0 => Dowload | 1 => Remover | 2 => Download Extra(Não foi preparado pelo capp)';
comment on domain DM_TIPO_ARQUIVO_LOJA is '0 => Produtos/Categorias... | 1 => Imagem | 2 => Imagem Full | 3 => Icone | 4 => Icone Secundario | 5 => Banner de divulgação';
comment on domain DM_TIPO_USUARIO is '0 => Livre | 1 => Monitoramento/Tester';
comment on domain DM_USO_STATUS is '1 => Em Uso | 0 => Sem Uso';


/* Criação das Exceptions */

CREATE EXCEPTION EXCEP_CONFIRMACAO_PED_DUPLICADA 'Pedido já confirmado/coletado!';
CREATE EXCEPTION EXCEP_PEDIDO_NOT_FOUND 'Pedido não encontrado!';
CREATE EXCEPTION EXCEP_MAX_EXIST_MAIOR_NOVO_MIN 'O valor mínimo definido é menor que um máximo já existente. Por favor, defina um mínimo maior.';
CREATE EXCEPTION EXCEP_USUARIO_INCOMPLETO 'Faltam informações para o cadastro de Usuário.';


/* Procedures utilizadas globalmente na base */

SET TERM ^ ;
CREATE PROCEDURE PR_SPLIT_STRING (
    P_STRING BLOB SUB_TYPE 1,
    P_LIMITADOR CHAR(1) )
RETURNS (
    PART BLOB SUB_TYPE 1 )
AS
DECLARE VARIABLE LASTPOS INTEGER;
DECLARE VARIABLE NEXTPOS INTEGER;
BEGIN
	P_STRING = :P_STRING || :P_LIMITADOR; LASTPOS = 1;
	NEXTPOS = POSITION(:P_LIMITADOR, :P_STRING, LASTPOS);

	IF (LASTPOS = NEXTPOS) THEN
	BEGIN
		PART = SUBSTRING(:P_STRING FROM :LASTPOS FOR :NEXTPOS - :LASTPOS);
		SUSPEND;
		LASTPOS = :NEXTPOS + 1;
		NEXTPOS = POSITION(:P_LIMITADOR, :P_STRING, LASTPOS);
	END

	WHILE (:NEXTPOS > 1) DO
	BEGIN
		PART = SUBSTRING(:P_STRING FROM :LASTPOS FOR :NEXTPOS - :LASTPOS);
		LASTPOS = :NEXTPOS + 1;
		NEXTPOS = POSITION(:P_LIMITADOR, :P_STRING, LASTPOS); SUSPEND;
	END
END^
SET TERM ; ^
comment on procedure PR_SPLIT_STRING is 'Procedure para dar split em uma string separada por determinado delimitador.';

--//

SET TERM ^ ;
CREATE PROCEDURE PR_FORMAT_HORARIO (
    HORARIO TIMESTAMP )
RETURNS (
    FORMATADO CHAR(19) )
AS
BEGIN
   FORMATADO = (lpad(extract(DAY from HORARIO), 2, '0') || '/' ||
        lpad(extract(MONTH from HORARIO), 2, '0') || '/' ||
        extract(YEAR from HORARIO) || ' ' ||
        lpad(extract(HOUR from HORARIO), 2, '0') || ':' ||
        lpad(extract(MINUTE from HORARIO), 2, '0') || ':' ||
        lpad(extract(SECOND from HORARIO), 2, '0'));


    FORMATADO = REPLACE(FORMATADO, '.', '0');--(bugfix) 00/00/0000 00:00:2. -> 00/00/0000 00:00:20


    SUSPEND;

END^
SET TERM ; ^
comment on procedure PR_FORMAT_HORARIO is 'Procedure para formatar o timestamp da base em um string. Ex.: 00/00/0000 00:00:00';

--//

SET TERM ^ ;
CREATE PROCEDURE PR_FORMAT_MONEY_REAL (
    VALOR DM_MONEY,
    NUMERO_CASAS INTEGER )
RETURNS (
    MONEY_REAL VARCHAR(50) )
AS
DECLARE VARIABLE VALOR_STR varchar(23);
DECLARE VARIABLE i int = 1;
DECLARE VARIABLE i_dot int = 1;
begin
    /**
    * Inspirado em http://mail.firebase.com.br/pipermail/lista_firebase.com.br/2006-July/030462.html
    **/

    VALOR_STR = cast(:valor as varchar(23));
    VALOR_STR = substring(:VALOR_STR from 1 for POSITION('.' in :VALOR_STR) - 1);


    i = CHAR_LENGTH(:VALOR_STR);
    MONEY_REAL = '';

    while (:i > 0) do
    BEGIN
        MONEY_REAL = substring(VALOR_STR from :i for 1) || MONEY_REAL;
        if (:i_dot = 3) then
        begin
            MONEY_REAL = '.' || MONEY_REAL ;
            i_dot = 0;
        end
        i = :i - 1;
        i_dot = :i_dot + 1;
    end


    MONEY_REAL = 'R$ ' || MONEY_REAL;


    VALOR_STR = cast(:valor as varchar(23));
    VALOR_STR = substring(:VALOR_STR from POSITION('.' in :VALOR_STR) + 1 for 4);

    MONEY_REAL = MONEY_REAL || ',';
    i = 1;

    while (:i <= :NUMERO_CASAS) do
    BEGIN
        MONEY_REAL = MONEY_REAL || substring(VALOR_STR from :i for 1);
        i = 1 + :i;
    end

    MONEY_REAL = REPLACE(MONEY_REAL, ' .', ' ');--(bugfix) R$ .100.000,00 -> R$ 100.000,00

    suspend;
end^
SET TERM ; ^
comment on procedure PR_FORMAT_MONEY_REAL is 'Procedure para formatar o tipo númerico da base para um string. Ex.: 1500.90 -> R$ 1.500,90';

--//

SET TERM ^ ;
CREATE PROCEDURE PR_FORMAT_GRUPO_MODULO (
    idgrupo integer,
    tablemodulo varchar(50),
    idmodulocolumn varchar(50),
    grupomodulocolumn varchar(50),
    controlemodulocolumn varchar(50))
returns (
    grupomodulo varchar(50))
as
declare variable sqlgrupoid integer;
declare variable sqlcontrolemodulo integer;
BEGIN
    EXECUTE STATEMENT
        'SELECT ' || grupoModuloColumn || ', ' || controleModuloColumn ||
            ' FROM ' || tableModulo ||
            ' WHERE ' || idModuloColumn || ' = ' || idGrupo
    INTO sqlGrupoId, sqlControleModulo;

    grupoModulo = cast(sqlControleModulo AS VARCHAR(50));

    WHILE (:sqlGrupoId IS NOT NULL) DO
    BEGIN
        idGrupo = sqlGrupoId;

        EXECUTE STATEMENT
            'SELECT ' || grupoModuloColumn || ', ' || controleModuloColumn ||
                ' FROM ' || tableModulo ||
                ' WHERE ' || idModuloColumn || ' = ' || idGrupo
        INTO sqlGrupoId, sqlControleModulo;

        grupoModulo = cast(sqlControleModulo AS VARCHAR(50)) || '.' || grupoModulo;
    END
    SUSPEND;
END^
SET TERM ; ^

--//

SET TERM ^ ;
CREATE PROCEDURE PR_GET_GRUPO_MODULO_ID (
    grupomodulo varchar(50),
    tablemodulo varchar(50),
    idmodulocolumn varchar(50),
    grupomodulocolumn varchar(50),
    controlemodulocolumn varchar(50))
returns (
    idgrupo integer,
    grupoprodutoid integer,
    controlemodulo integer)
as
declare variable lastpos integer;
declare variable nextpos integer;
declare variable tempsrt varchar(50);
declare variable sqlgrupoid integer;
BEGIN
    grupoModulo = grupoModulo || '.';
    lastPos = 1;
    nextPos = position('.', grupoModulo, lastPos);

    IF (nextPos > 1) THEN
    BEGIN
        idGrupo = 0;
        tempSrt = SUBSTRING(grupoModulo FROM lastPos FOR nextPos - lastPos);

        EXECUTE STATEMENT
            'SELECT ' || idModuloColumn || ', ' || grupoModuloColumn || ', ' || controleModuloColumn ||
                ' FROM ' || tableModulo ||
                ' WHERE ' || controleModuloColumn || ' = ' || tempSrt ||
                ' AND ' || grupoModuloColumn || ' IS NULL'
        INTO idGrupo, grupoProdutoId, controleModulo;

        lastPos = :nextPos + 1;
        nextPos = POSITION('.', grupoModulo, lastPos);
    END

    WHILE (nextPos > 1) DO
    BEGIN
        sqlGrupoId = 0;
        tempSrt = SUBSTRING(:grupoModulo FROM lastPos FOR nextPos - lastPos);

        EXECUTE STATEMENT
            'SELECT ' || idModuloColumn || ', ' || grupoModuloColumn || ', ' || controleModuloColumn ||
                ' FROM ' || tableModulo ||
                ' WHERE ' || controleModuloColumn || ' = ' || tempSrt ||
                ' AND ' || grupoModuloColumn || ' = ' || idGrupo
        INTO sqlGrupoId, grupoProdutoId, controleModulo;

        idGrupo = sqlGrupoId;
        lastPos = nextPos + 1;
        nextPos = POSITION('.', grupoModulo, lastPos);
    END
    SUSPEND;
END^
SET TERM ; ^

--//

SET TERM ^ ;
CREATE PROCEDURE PR_FORMAT_MODULO_DESCRICAO (
    idgrupo integer,
    tablemodulo varchar(50),
    idmodulocolumn varchar(50),
    grupomodulocolumn varchar(50),
    descricaocolumn varchar(1000))
returns (
    grupomodulo varchar(1000))
as
declare variable sqlgrupoid integer;
declare variable descricaomodulo varchar(1000);
BEGIN
    EXECUTE STATEMENT
        'SELECT ' || grupoModuloColumn || ', ' || descricaoColumn ||
            ' FROM ' || tableModulo ||
            ' WHERE ' || idModuloColumn || ' = ' || idGrupo
    INTO sqlGrupoId, descricaoModulo;

    grupoModulo = descricaoModulo;

    WHILE (:sqlGrupoId IS NOT NULL) DO
    BEGIN
        idGrupo = sqlGrupoId;

        EXECUTE STATEMENT
            'SELECT ' || grupoModuloColumn || ', ' || descricaoColumn ||
                ' FROM ' || tableModulo ||
                ' WHERE ' || idModuloColumn || ' = ' || idGrupo
        INTO sqlGrupoId, descricaoModulo;

        grupoModulo = descricaoModulo || ' / ' || grupoModulo;
    END
    SUSPEND;
END^
SET TERM ; ^

--//

SET TERM ^ ;
CREATE PROCEDURE PR_GET_GRUPO_MAIOR_STRING (
    tablemodulo varchar(50),
    idmodulocolumn varchar(50),
    grupomodulocolumn varchar(50),
    controlemodulocolumn varchar(50))
returns (
    maiorquant varchar(50))
as
declare variable idgrup integer;
declare variable grupomodulo varchar(50);
BEGIN
    MAIORQUANT = '';
    FOR
        EXECUTE STATEMENT
        'SELECT '|| IDMODULOCOLUMN ||' FROM '||TABLEMODULO
        INTO IDGRUP
    DO
    BEGIN
        EXECUTE PROCEDURE
            PR_FORMAT_GRUPO_MODULO IDGRUP, TABLEMODULO, IDMODULOCOLUMN, GRUPOMODULOCOLUMN, CONTROLEMODULOCOLUMN
            RETURNING_VALUES GRUPOMODULO;

            IF (CHAR_LENGTH(GRUPOMODULO) > CHAR_LENGTH(MAIORQUANT)) THEN
            BEGIN
                MAIORQUANT = GRUPOMODULO;
            END
    END

    SUSPEND;
END^
SET TERM ; ^

--//

SET TERM ^ ;
CREATE PROCEDURE PR_GET_NEXT_GRUPO_MODULO (
    grupomodulo varchar(50),
    tablemodulo varchar(50),
    idmodulocolumn varchar(50),
    grupomodulocolumn varchar(50),
    controlemodulocolumn varchar(50))
returns (
    nextcontrolemodulo varchar(1000))
as
declare variable idgrupo integer;
declare variable grupoprodutoid integer;
declare variable controlemodulo integer;
BEGIN
    EXECUTE PROCEDURE PR_GET_GRUPO_MODULO_ID
        :grupoModulo, :tableModulo, :idModuloColumn, :grupoModuloColumn, :controleModuloColumn
    RETURNING_VALUES
        idGrupo, grupoProdutoId, controleModulo;

    controleModulo = 1;

    IF (idGrupo <> 0) THEN
    BEGIN
        EXECUTE STATEMENT
            'SELECT MAX(' || controleModuloColumn || ') + 1' ||
            ' FROM ' || tableModulo ||
            ' WHERE ' || grupoModuloColumn || ' = ' || idGrupo
        INTO controleModulo;

        IF (controleModulo IS NULL) THEN
        BEGIN
            controleModulo = 1;
        END
    END

    nextControleModulo = grupoModulo || '.' || cast(controleModulo AS VARCHAR(50));
    SUSPEND;
END^
SET TERM ; ^

--//

SET TERM ^ ;
CREATE PROCEDURE PR_FORMAT_DECIMAL_PRECO (
    valor decimal(18,4),
    numero_casas integer)
returns (
    money_real varchar(50))
as
declare variable valor_str varchar(23);
declare variable i integer = 1;
BEGIN
    VALOR_STR = cast(VALOR AS VARCHAR(23));
    VALOR_STR = substring(VALOR_STR FROM 1 FOR POSITION('.' IN VALOR_STR) - 1);

    MONEY_REAL = VALOR_STR || '.';

    VALOR_STR = cast(VALOR AS VARCHAR(23));
    VALOR_STR = substring(VALOR_STR FROM POSITION('.' IN VALOR_STR) + 1 FOR 4);

    WHILE (I <= NUMERO_CASAS) DO
    BEGIN
        MONEY_REAL = MONEY_REAL || SUBSTRING(VALOR_STR FROM I FOR 1);
        I = 1 + I;
    END
    SUSPEND;
END^

SET TERM ; ^
