SET TERM ^ ;
CREATE PROCEDURE PR_VALOR_PEDIDO_ITEM (
    PEDIDO_ITEM_ID integer )
RETURNS (
    TOTAL DM_MONEY )
AS
declare VARIABLE tmp DM_MONEY;
BEGIN
    tmp = 0;
    select
        coalesce(pi.VALOR_UNIDADE_COBRADO, 0)
    from
        PEDIDO_ITEM pi
    where
        pi.ID = :PEDIDO_ITEM_ID
    into tmp;
    TOTAL = coalesce(:tmp, 0);
    select
        sum(coalesce(pii.VALOR_COBRADO, 0))
    from
        PEDIDO_ITEM_INGR pii
    where
        pii.PEDIDO_ITEM_ID = :PEDIDO_ITEM_ID
    and
        pii.IS_CORTESIA <> 1
    into tmp;
   TOTAL = coalesce(:tmp, 0) + :total;
    select
        sum(coalesce(piop.VALOR_COBRADO, 0))
    from
        PEDIDO_ITEM_OPCIONAL_PROD piop
    where
        piop.PEDIDO_ITEM_ID = :PEDIDO_ITEM_ID
    into tmp;
   TOTAL = coalesce(:tmp, 0) + :total;
    select
       sum(coalesce(pisp.VALOR_COBRADO, 0)) + sum(coalesce(pisi.VALOR_COBRADO, 0))
    from
        PEDIDO_ITEM_SABOR_PROD2 pisp
    LEFT JOIN PEDIDO_ITEM_SABOR_INGR2 pisi
        ON pisi.PIS_PEDIDO_SABOR_PROD_ID = pisp.ID
    where
        pisp.PEDIDO_ITEM_ID = :PEDIDO_ITEM_ID
    into tmp;
  TOTAL = coalesce(:tmp, 0) + :total;
    suspend;
END^
SET TERM ; ^
GRANT EXECUTE
 ON PROCEDURE PR_VALOR_PEDIDO_ITEM TO  EGULAWEB;
GRANT EXECUTE
 ON PROCEDURE PR_VALOR_PEDIDO_ITEM TO  MONITORAMENTO;
GRANT EXECUTE
 ON PROCEDURE PR_VALOR_PEDIDO_ITEM TO  SYSDBA;


SET TERM ^ ;
CREATE PROCEDURE PR_VALOR_TOTAL_PEDIDO (
    PEDIDO_ID integer )
RETURNS (
    VALOR_TOTAL DM_MONEY )
AS
BEGIN
select
        coalesce(
            sum(
                (SELECT coalesce(TOTAL, 0) FROM PR_VALOR_PEDIDO_ITEM (pi.ID)
            ) * pi.QUANTIDADE
        ), 0) as valor_total
    from
        PEDIDO p
    join
        PEDIDO_ITEM pi on pi.PEDIDO_ID = p.ID
    where
        p.ID = :PEDIDO_ID
    into VALOR_TOTAL;

    VALOR_TOTAL = :VALOR_TOTAL + (select (
        COALESCE(p.VALOR_ENTREGA_COBRADO, 0)
        + COALESCE(p.VALOR_PAGAMENTO_COBRADO, 0)
        + COALESCE(p.VALOR_ACRESCIMO_COBRADO, 0)
    ) from PEDIDO p where p.ID = :PEDIDO_ID);
    suspend;
END^
SET TERM ; ^



alter table usuario add nome_completo computed by (NOME || ' ' || SOBRENOME);
alter table pedido add total computed by (iif(TOTAL_TMP is null, (select VALOR_TOTAL from PR_VALOR_TOTAL_PEDIDO (id)), TOTAL_TMP));



SET TERM ^ ;
CREATE PROCEDURE PR_ADMDEL_SERVIDOR_CAPP (
    HOST DM_HOST_PORT )
AS
BEGIN

      IN AUTONOMOUS TRANSACTION DO
        BEGIN
            DELETE FROM SERVIDOR s WHERE s.HOST = :HOST;
        end
        SUSPEND;

END^
SET TERM ; ^


SET TERM ^ ;
CREATE  PROCEDURE PR_ADMUP_SERVIDOR_CAPP (
    TITULO DM_NOME,
    HOST_EXTERNO DM_HOST_PORT,
    HOST_INTERNO DM_HOST_PORT,
    IS_DEFAULT DM_BOOL,
    INDICE SMALLINT )
AS
declare variable COUNT_SERVIDORES integer;
BEGIN

    SELECT
        COUNT(1)
    FROM
        SERVIDOR s
    WHERE
        s.HOST = :HOST_EXTERNO
    into :COUNT_SERVIDORES;

    IN AUTONOMOUS TRANSACTION DO
    BEGIN
        if (:COUNT_SERVIDORES = 0) then
        begin
            insert into SERVIDOR(DESCRICAO, HOST, IS_MASTER)
            values (:TITULO, :HOST_EXTERNO, :IS_DEFAULT);
        end
        else
        begin
            UPDATE SERVIDOR SET
            DESCRICAO = :TITULO,
            IS_MASTER = :IS_DEFAULT
            WHERE HOST = :HOST_EXTERNO;
        end

    end
    SUSPEND;

    END^
SET TERM ; ^
comment on procedure PR_ADMUP_SERVIDOR_CAPP is 'Insere ou atualiza registro da tabela servidor.';

CREATE VIEW VW_ARQUIVO_LOJA_FORCADO (CNPJ, ARQUIVO_DISPONIVEL_ID, PATH, NAME,
    NAME_TO)
AS
    SELECT
        l.CNPJ,
        ad.ID,
        ad.PATH,
        ad.APP_PATH,
        'categorias-' || l.CNPJ AS NOME_TO
    FROM
        ARQUIVO_DISPONIVEL ad
    INNER JOIN
        LOJA_ARQUIVO_DISPONIVEL lad
            ON lad.ARQUIVO_DISPONIVEL_ID = ad.ID
     INNER JOIN
        LOJA l
            ON lad.LOJA_ID = l.ID
    WHERE
        lad.TIPO = 0
        AND ad.EXISTE = 1
        AND (
                (ad.PATH LIKE '%categoria%.json' and (l.IS_FUNCIONAMENTO_FX is null or l.IS_FUNCIONAMENTO_FX = 0))
                or
                (ad.PATH LIKE '%cardapio%.json' and l.IS_FUNCIONAMENTO_FX = 1)
            )

;

SET TERM ^ ;
CREATE PROCEDURE PR_ARQUIVOS_LOJA_PENDENTES (
    ID_DEV BIGINT,
    CNPJ DM_CNPJ )
RETURNS (
    ARQUIVO_DISPONIVEL_ID BIGINT,
    PATH VARCHAR(250),
    NAME_TO VARCHAR(150) )
AS
BEGIN

    FOR
    SELECT DISTINCT
        v.ARQUIVO_DISPONIVEL_ID,
        v.NAME,
        v.NAME_TO
    FROM
        VW_ARQUIVO_LOJA_FORCADO v
    where
        v.CNPJ = :CNPJ
        and (
            not EXISTS(
                select
                    1
                from
                    ARQUIVO_DOWNLOAD ad
                inner join DISPOSITIVO_ACAO da
                    on da.ID = ad.DISPOSITIVO_ACAO_ID
                where
                    ad.ARQUIVO_DISPONIVEL_ID = v.ARQUIVO_DISPONIVEL_ID
                    and da.DISPOSITIVO_ID = :id_dev

            )
            or EXISTS(
                select
                    1
                from
                    ARQUIVO_DOWNLOAD ad
                inner join DISPOSITIVO_ACAO da
                    on da.ID = ad.DISPOSITIVO_ACAO_ID AND ad.HORARIO_FIM_DOWNLOAD IS NULL
                where
                    ad.ARQUIVO_DISPONIVEL_ID = v.ARQUIVO_DISPONIVEL_ID
                    and da.DISPOSITIVO_ID = :id_dev
                     and not EXISTS(
                            select
                               1
                            from
                                ARQUIVO_DOWNLOAD ad2
                            inner join DISPOSITIVO_ACAO da2
                                on da2.ID = ad2.DISPOSITIVO_ACAO_ID AND ad2.HORARIO_FIM_DOWNLOAD IS not NULL
                            where
                                ad2.ARQUIVO_DISPONIVEL_ID = ad.ARQUIVO_DISPONIVEL_ID
                                and da2.DISPOSITIVO_ID = da.DISPOSITIVO_ID
                                and da2.HORARIO > da.HORARIO
                        )

            )
        )
    INTO
         :ARQUIVO_DISPONIVEL_ID, :PATH , :NAME_TO
    do
    begin

        SUSPEND;
    end


END^
SET TERM ; ^
comment on procedure PR_ARQUIVOS_LOJA_PENDENTES is 'Retorna os arquivos(não imagens) da loja que o dispositivo precisa';


SET TERM ^ ;
CREATE PROCEDURE PR_RETORNA_CADASTRO_DISPOSITIVO (
    ID_DEV VARCHAR(100) )
RETURNS (
    ID BIGINT )
AS
declare variable SO char(1);
BEGIN
   ID_DEV = LOWER(:ID_DEV);

    ID = null;

    SELECT FIRST 1 r.ID FROM DISPOSITIVO r WHERE r.ID_HARDWARE = :ID_DEV into :ID;

    if (ID is null) then
    begin
        SO = 0;
        if (substring(:ID_DEV from 1 for 2) = 'io') then
            SO = 1;

        INSERT into DISPOSITIVO (HORARIO_CADASTRO, SO,ID_HARDWARE,
        FORCE_LOGOFF_PENDENTE) VALUES ('now', :SO, :ID_DEV,0)
        RETURNING ID into :ID;

    end

    SUSPEND;
END^
SET TERM ; ^
comment on procedure PR_RETORNA_CADASTRO_DISPOSITIVO is 'Cadastra, caso não exista, e retorna o cadastro do dispositivo com base no seu id dev';


SET TERM ^ ;
CREATE PROCEDURE PR_RETORNA_CREDITO_LOJA (
    LOJA_ID INTEGER )
RETURNS (
    CREDITO DM_MONEY )
AS
DECLARE VARIABLE TOTAL_VENDIDO DM_MONEY;
BEGIN

    SELECT
        SUM (p.TOTAL)
    FROM
        PEDIDO p
    WHERE
        p.LOJA_ENDERECO_LOJA_ID = :LOJA_ID
        AND p.IS_CANCELADO = 0
    INTO
        TOTAL_VENDIDO;

    SELECT FIRST 1
        l.CREDITO
    FROM
        LOJA_CREDITO l
    WHERE
        l.LOJA_ID = :LOJA_ID
    ORDER BY
        l.CADASTRO DESC
    INTO
        CREDITO;

    CREDITO = IIF(:CREDITO IS NULL, 0, :CREDITO) - IIF(:TOTAL_VENDIDO IS NULL, 0, :TOTAL_VENDIDO);

    SUSPEND;

END^
SET TERM ; ^


SET TERM ^ ;
CREATE PROCEDURE PR_CADASTRA_DISPOSITIVO (
    ID_DEV VARCHAR(100) )
AS
declare VARIABLE ID BIGINT;
BEGIN

     execute PROCEDURE PR_RETORNA_CADASTRO_DISPOSITIVO(:ID_DEV) RETURNING_VALUES :ID;

END^
SET TERM ; ^
comment on procedure PR_CADASTRA_DISPOSITIVO is 'Cadastra, caso não exista, o dispositivo com base no seu id dev';


SET TERM ^ ;
CREATE PROCEDURE PR_CONFIRMA_PEDIDO (
    ID BIGINT,
    MG_HOST_CLIENTE DM_HOST_PORT,
    MG_SC_ID INTEGER,
    PREVISAO_ENTREGA TIMESTAMP )
AS
declare variable acao_id bigint;
declare variable pago_em_bonus DM_BOOL;
BEGIN

    /**
    * Confirmação do pedido
    */


    if ((select count(0) from PEDIDO p where p.ID = :ID) = 0) then
    BEGIN
        EXCEPTION EXCEP_PEDIDO_NOT_FOUND;
    end


    if ((select count(0) from PEDIDO p where p.ID = :ID and p.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null) > 0) then
    BEGIN
        EXCEPTION EXCEP_CONFIRMACAO_PED_DUPLICADA;
    end

    INSERT INTO MONITOR_SC_ACAO(HORARIO, HOST_CLIENT,MONITOR_SC_ID)
        values ('now',:MG_HOST_CLIENTE, :MG_SC_ID) RETURNING ID into :acao_id;

    update PEDIDO p set p.MONITOR_SC_CONFIRMADOR_ACAO_ID = :acao_id, p.HORARIO_ESTIMADO_ENTREGA = :PREVISAO_ENTREGA
        where p.ID = :ID;
    --Bugfix: caso o mg não tenha enviado ao capp o aviso de que recebeu o pedido
    update PEDIDO p set p.MONITOR_SC_COLETOR_ACAO_ID = :acao_id
        where p.ID = :ID AND p.MONITOR_SC_COLETOR_ACAO_ID is null;

END^
SET TERM ; ^
comment on procedure PR_CONFIRMA_PEDIDO is 'Efetua a confirmação de um pedido';


SET TERM ^ ;
CREATE PROCEDURE PR_CONFIRMA_RECEBIMENTO_PEDIDO (
    IDS VARCHAR(500),
    MG_HOST_CLIENTE DM_HOST_PORT,
    MG_SC_ID INTEGER )
AS
DECLARE VARIABLE acao_id BIGINT;
DECLARE VARIABLE ID BIGINT;
BEGIN
    INSERT INTO MONITOR_SC_ACAO(HORARIO, HOST_CLIENT,MONITOR_SC_ID)
        values ('now', :MG_HOST_CLIENTE, :MG_SC_ID) RETURNING ID into :acao_id;

     for
        SELECT
            CAST(p.PART as BIGINT)
        FROM
            PR_SPLIT_STRING (:IDS, ',') p
        INTO
            :ID
    do
    begin
        update PEDIDO p set p.MONITOR_SC_COLETOR_ACAO_ID = :acao_id
            where p.ID = :ID and p.MONITOR_SC_COLETOR_ACAO_ID is null;
    end
END^
SET TERM ; ^
comment on procedure PR_CONFIRMA_RECEBIMENTO_PEDIDO is 'Efetua a confirmação do recebimento de um pedido pelo MG';


SET TERM ^ ;
CREATE PROCEDURE PR_FIX_LOJA_ANTIGO_CAPP_TMP (
    CNPJ DM_CNPJ,
    STATUS_LOJA DM_LOJA_STATUS,
    BONUS_CONF DM_MONEY,
    NOME DM_NOME )
AS
declare variable id_loja INTEGER = 0;
BEGIN

    -- Chamada pelo antigo capp para corrigir valores sobre o contratante
    -- Remover isso algum dia!

    SELECT r.ID FROM LOJA r
    where r.CNPJ = :CNPJ
    into :id_loja;

    if (:id_loja > 0) then
    begin
        UPDATE LOJA SET
            LOJA_STATUS = :STATUS_LOJA,
            NOME = :NOME,
            LAST_UPDATE_INFOS = 'now'
        WHERE
            CNPJ = :CNPJ;


        if ((SELECT
                count(1)
                FROM
                    LOJA_BONUS_CONF r
                where
                    r.LOJA_ID = :id_loja
                    and r.STATUS_BONUS = 1
                    and r.PERCENTUAL = :BONUS_CONF) = 0) then
        begin
                UPDATE LOJA_BONUS_CONF SET
                    STATUS_BONUS = 0
                WHERE
                    LOJA_ID = :id_loja;

                if ((SELECT
                        count(1)
                        FROM
                            LOJA_BONUS_CONF r
                        where
                            r.LOJA_ID = :id_loja
                            and r.PERCENTUAL = :BONUS_CONF) = 1) then
                begin
                        UPDATE LOJA_BONUS_CONF SET
                            STATUS_BONUS = 1
                        WHERE
                            LOJA_ID = :id_loja
                            and PERCENTUAL = :BONUS_CONF;
                end
                else
                begin
                        INSERT INTO LOJA_BONUS_CONF (LOJA_ID, PERCENTUAL, STATUS_BONUS)
                        VALUES (
                            :id_loja,
                            :BONUS_CONF,
                            1
                        );

                end


        end

    end
END^
SET TERM ; ^
comment on procedure PR_FIX_LOJA_ANTIGO_CAPP_TMP is 'Chamada pelo antigo capp para corrigir valores sobre o contratante';



SET TERM ^ ;
create PROCEDURE PR_RETORNA_BONUS_USUARIO (
    USER_ID INTEGER )
RETURNS (
    TOTAL_BONUS_GERADO DM_MONEY,
    TOTAL_BONUS_BLOQUEADO DM_MONEY,
    TOTAL_BONUS_LIVRE DM_MONEY,
    TOTAL_BONUS_USADO DM_MONEY )
AS
BEGIN

 SELECT
        sum(
            (r.TOTAL / 100) * lbc.PERCENTUAL
        ),
        sum(
           iif(r.PAGO_EM_BONUS = 1, r.TOTAL, 0)
        ),
        sum(
           iif(r.MONITOR_SC_CONFIRMADOR_ACAO_ID is null, (r.TOTAL / 100) * lbc.PERCENTUAL, 0)
        ),
        sum(
          iif(r.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null and r.MONITOR_SC_FALHA_ACAO_ID is null, (r.TOTAL / 100) * lbc.PERCENTUAL, 0)
        )
    FROM
        PEDIDO r
    inner join LOJA_BONUS_CONF lbc
        on lbc.LOJA_ID = r.LOJA_ENDERECO_LOJA_ID
    where
        r.USUARIO_ENDERECO_USUARIO_ID = :user_id
        and lbc.ID = r.LOJA_BONUS_CONF_ID
    into :TOTAL_BONUS_GERADO, :TOTAL_BONUS_USADO,:TOTAL_BONUS_BLOQUEADO,:TOTAL_BONUS_LIVRE;

    TOTAL_BONUS_LIVRE = :TOTAL_BONUS_LIVRE - :TOTAL_BONUS_USADO;

    TOTAL_BONUS_GERADO = COALESCE(:TOTAL_BONUS_GERADO, 0);
    TOTAL_BONUS_USADO = COALESCE(:TOTAL_BONUS_USADO, 0);
    TOTAL_BONUS_BLOQUEADO = COALESCE(:TOTAL_BONUS_BLOQUEADO, 0);
    TOTAL_BONUS_LIVRE = COALESCE(:TOTAL_BONUS_LIVRE, 0);

  SUSPEND;

END^
SET TERM  ; ^
comment on procedure PR_RETORNA_BONUS_USUARIO is 'USAR vw_bonus_livre_usuario!';

create view vw_bonus_livre_usuario(USUARIO_ID, BONUS)
as
SELECT
    r.USUARIO_ENDERECO_USUARIO_ID,
    sum(
      iif(r.IS_CANCELADO = 0 and r.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null and r.MONITOR_SC_FALHA_ACAO_ID is null, (r.TOTAL / 100) * lbc.PERCENTUAL, 0)
    ) -
    sum(
       iif(r.IS_CANCELADO = 0 and r.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null and r.MONITOR_SC_FALHA_ACAO_ID is null and r.PAGO_EM_BONUS = 1, r.TOTAL, 0)
    ) 
FROM
    PEDIDO r
inner join LOJA_BONUS_CONF lbc
    on lbc.LOJA_ID = r.LOJA_ENDERECO_LOJA_ID
where
    lbc.ID = r.LOJA_BONUS_CONF_ID
group by 1

comment on view vw_bonus_livre_usuario is 'Calcula e retorna o bônus livre do usuário';


CREATE VIEW VW_INFOS_USUARIO (ID, HORARIO_CADASTRO, CPF, NOME, SOBRENOME,
    EMAIL, TIPO_USUARIO, FACEBOOK_ID, TELEFONE_NUMERO, TOTAL_BONUS_BLOQUEADO,
    TOTAL_BONUS_GERADO, TOTAL_BONUS_LIVRE, TOTAL_BONUS_USADO)
AS
    SELECT
        r.ID,
        da.HORARIO as HORARIO_CADASTRO,
        r.CPF,
        r.NOME,
        r.SOBRENOME,
        r.EMAIL,
        r.TIPO_USUARIO,
        r.FACEBOOK_ID,
        t.NUMERO as TELEFONE_NUMERO,
        bu.TOTAL_BONUS_BLOQUEADO,
        bu.TOTAL_BONUS_GERADO,
        bu.TOTAL_BONUS_LIVRE,
        bu.TOTAL_BONUS_USADO
    FROM
        USUARIO r
    inner join DISPOSITIVO_ACAO da
        on da.ID = r.DISPOSITIVO_ACAO_CADASTRO_ID
    left join PR_RETORNA_BONUS_USUARIO(r.ID) bu
        on 1 = 1
    left join TELEFONE t
        on t.ID = (
            select
                first 1
                ut.TELEFONE_ID
            from
                USUARIO_TELEFONE ut
            where ut.USUARIO_ID = r.ID
            order by ut.ID desc
        )
    ;


CREATE VIEW VW_INFOS_ENDERECO (ID, LOGRADOURO, NUMERO, COMPLEMENTO, REFERENCIA,
    CEP, LATITUDE, LONGITUDE, BAIRRO, BAIRRO_ID, CIDADE, CIDADE_ID, ESTADO,
    ESTADO_ID, PAIS, PAIS_ID)
AS
SELECT
    e.ID,
    e.LOGRADOURO,
    e.NUMERO,
    e.COMPLEMENTO,
    e.REFERENCIA,
    e.CEP,
    e.LATITUDE,
    e.LONGITUDE,
    b.NOME as BAIRRO,
    b.ID as bairro_id,
    c.NOME as cidade,
    c.id as CIDADE_ID,
    uf.SIGLA as estado,
    uf.ID as estado_id,
    p.NOME as pais,
    p.id as pais_id
FROM
    ENDERECO e
inner join BAIRRO b
    on b.ID = e.BAIRRO_ID
inner join CIDADE c
    on c.ID = b.CIDADE_ID
inner join ESTADO uf
    on uf.ID = c.ESTADO_ID
inner join PAIS p
    on p.ID = uf.PAIS_ID;


CREATE VIEW VW_INFOS_USUARIO_ENDERECO (USUARIO_ID, ID, LOGRADOURO, NUMERO,
    COMPLEMENTO, REFERENCIA, CEP, LATITUDE, LONGITUDE, BAIRRO, BAIRRO_ID,
    CIDADE, CIDADE_ID, ESTADO, ESTADO_ID, PAIS, PAIS_ID)
AS

    SELECT
        ue.USUARIO_ID,
        r.ID,
        r.LOGRADOURO,
        r.NUMERO,
        r.COMPLEMENTO,
        r.REFERENCIA,
        r.CEP,
        r.LATITUDE,
        r.LONGITUDE,
        r.BAIRRO,
        r.BAIRRO_ID,
        r.CIDADE,
        r.CIDADE_ID,
        r.ESTADO,
        r.ESTADO_ID,
        r.PAIS,
        r.PAIS_ID
    FROM
        USUARIO_ENDERECO ue
    inner join VW_INFOS_ENDERECO r
        on r.ID = ue.ENDERECO_ID
    left join LOJA_ENDERECO le
        on r.ID = le.ENDERECO_ID
    where
        le.ENDERECO_ID is null
        AND ue.APP_STATUS <> 0;


CREATE VIEW VW_INFOS_USUARIO_TELEFONE (USUARIO_ID, ID, TELEFONE_ID, NUMERO,
    OPERADORA)
AS
    SELECT
        ut.USUARIO_ID,
        ut.ID,
        ut.TELEFONE_ID,
        t.NUMERO,
        t.OPERADORA
    FROM
        USUARIO_TELEFONE ut
    inner join TELEFONE t
        on t.ID = ut.TELEFONE_ID;
CREATE VIEW VW_INFOS_LOJA (ID, CNPJ, NOME, LOJA_STATUS,
    APP_INDICE_APRESENTACAO, ENDERECO_ID, LOGRADOURO, NUMERO, COMPLEMENTO,
    REFERENCIA, CEP, LATITUDE, LONGITUDE, BAIRRO, BAIRRO_ID, CIDADE, CIDADE_ID,
    ESTADO, ESTADO_ID, PAIS, PAIS_ID, PERCENTUAL, PERCENTUAL_ID)
AS
SELECT
    r.ID,
    r.CNPJ,
    r.NOME,
    r.LOJA_STATUS,
    r.APP_INDICE_APRESENTACAO,
    e.ID as ENDERECO_ID,
    e.LOGRADOURO,
    e.NUMERO,
    e.COMPLEMENTO,
    e.REFERENCIA,
    e.CEP,
    e.LATITUDE,
    e.LONGITUDE,
    e.BAIRRO,
    e.bairro_id,
    e.cidade,
    e.CIDADE_ID,
    e.estado,
    e.estado_id,
    e.pais,
    e.pais_id,
    lbf.PERCENTUAL,
    lbf.ID as PERCENTUAL_ID
FROM
    LOJA r
inner join LOJA_ENDERECO le
    on le.LOJA_ID = r.ID
inner join VW_INFOS_ENDERECO e
    on e.ID = le.ENDERECO_ID
INNER join LOJA_BONUS_CONF lbf
    on lbf.LOJA_ID = r.id
where
    le.STATUS_ENDERECO = 1
    and
    lbf.STATUS_BONUS = 1
;




CREATE VIEW VW_INFOS_DISPOSITIVO (ID, APP_VERSAO, APP_TOKEN_FCM,
    HORARIO_CADASTRO, MODELO, FABRICANTE, SO, SO_VERSAO, SDK_VERSAO,
    TELEFONE_NUMERO, TELEFONE_OPERADORA, ID_HARDWARE, HORARIO_ULTIMA_ACAO)
AS
SELECT
    d.ID,
    COALESCE((
        select
            first 1
            dav.APP_VERSAO
        from
            DISPOSITIVO_ACAO da
        INNER join DISPOSITIVO_APP_VERSAO dav
            on dav.DISPOSITIVO_ACAO_ID = da.ID
        WHERE
            da.DISPOSITIVO_ID = d.ID
        ORDER BY
            da.ID DESC
    ), null) as APP_VERSAO,
    COALESCE((
         select
            first 1
            dat.APP_TOKEN_FCM
        from
            DISPOSITIVO_ACAO da
        INNER join DISPOSITIVO_APP_TOKEN_FCM dat
            on dat.DISPOSITIVO_ACAO_ID = da.ID
        WHERE
            da.DISPOSITIVO_ID = d.ID
        ORDER BY
            da.ID DESC
    ), null) as APP_TOKEN_FCM,
    d.HORARIO_CADASTRO,
    d.MODELO,
    d.FABRICANTE,
    d.SO,
    d.SO_VERSAO,
    d.SDK_VERSAO,
    tl.NUMERO as TELEFONE_NUMERO,
    tl.OPERADORA as TELEFONE_OPERADORA,
    d.ID_HARDWARE,
    (
        select
            first 1
            da.HORARIO
        from
            DISPOSITIVO_ACAO da
        where
            da.DISPOSITIVO_ID = d.ID
        order by
            da.HORARIO desc
    ) as HORARIO_ULTIMA_ACAO
FROM
    DISPOSITIVO d
left join TELEFONE tl
    on tl.ID = d.TELEFONE_ID;



SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_ENDERECO (
    LOGRADOURO DM_DESCRICAO,
    NUMERO VARCHAR(10),
    COMPLEMENTO DM_DESCRICAO,
    REFERENCIA DM_REFERENCIA,
    CEP DM_CEP,
    LATITUDE DECIMAL(11,8),
    LONGITUDE DECIMAL(11,8),
    BAIRRO DM_NOME,
    BAIRRO_ALT_ID INTEGER,
    CIDADE DM_NOME,
    CIDADE_ALT_ID INTEGER,
    ESTADO DM_NOME,
    ESTADO_ALT_ID INTEGER,
    PAIS DM_NOME,
    PAIS_ALT_ID INTEGER )
RETURNS (
    ID INTEGER )
AS
declare variable id_pais int;
declare variable id_uf int;
declare variable id_cidade int;
declare variable id_bairro int;
DECLARE VARIABLE ibge_cidade int;
BEGIN
    IN AUTONOMOUS TRANSACTION DO
    BEGIN
        /**
        * Os parâmetros '_ALT_ID' quando usados indicam que a informação já possuí
        * um id, logo o mesmo deverá ser usado
        */

        -- País
        if (:PAIS_ALT_ID is null) then
        begin
            SELECT first 1 r.ID FROM PAIS r where
                lower(r.NOME) like '%' || lower(:pais) || '%' into :id_pais;

            if (:id_pais is null) then
            begin
                insert into pais(NOME) values (:pais) RETURNING ID into :id_pais;
            end
        end
        else
        begin
            id_pais = :PAIS_ALT_ID;
        end

        -- Estado/UF/Região
        if (:ESTADO_ALT_ID is null) then
        begin
            SELECT first 1 r.ID FROM ESTADO r where
                (lower(r.NOME) like '%' || lower(:estado) || '%'
                OR lower(r.SIGLA) like '%' || lower(SUBSTRING(:estado from 1 for 2)) || '%')
                AND r.PAIS_ID = :id_pais into :id_uf;

            if (:id_uf is null) then
            begin
                insert into ESTADO(NOME, PAIS_ID, SIGLA) values
                    (:estado, :id_pais,SUBSTRING(:estado from 1 for 2))
                    RETURNING ID into :id_uf;
            end
        end
        else
        begin
            id_uf = :ESTADO_ALT_ID;
        end

        -- Cidade
        if (:CIDADE_ALT_ID is null) then
        begin
            --Quando se trata de um código ibge
            ibge_cidade = -1;
            if ((select char_length(:cidade) from rdb$database) = 7) then
            begin
                ibge_cidade = CAST(:cidade AS INTEGER);
                WHEN ANY DO ibge_cidade = -1;
            end


            if ( :ibge_cidade > -1) then
            begin
                --Quando é o ibge da cidade
                SELECT first 1 r.ID FROM CIDADE r where
                    r.CODIGO_IBGE = :cidade
                    AND r.ESTADO_ID = :id_uf into :id_cidade;

                if (:id_cidade is null) then
                begin
                    insert into CIDADE(NOME, ESTADO_ID, CODIGO_IBGE) values
                        ('Desconhecida', :id_uf, :cidade)
                        RETURNING ID into :id_cidade;
                end

            end
            else
            begin
                --Quando é apenas o nome da cidade
                SELECT first 1 r.ID FROM CIDADE r where
                    lower(r.NOME) like '%' || lower(:cidade) || '%'
                    AND r.ESTADO_ID = :id_uf into :id_cidade;

                if (:id_cidade is null) then
                begin
                    insert into CIDADE(NOME, ESTADO_ID) values
                        (:cidade, :id_uf)
                        RETURNING ID into :id_cidade;
                end
            end
        end
        else
        begin
            id_cidade = :CIDADE_ALT_ID;
        end

        -- Bairro
        if (:BAIRRO_ALT_ID is null) then
        begin
            SELECT first 1 r.ID FROM BAIRRO r where
                lower(r.NOME) like '%' || lower(:BAIRRO) || '%'
                AND r.CIDADE_ID = :id_cidade into :id_bairro;

            if (:id_bairro is null) then
            begin
                insert into BAIRRO(NOME, CIDADE_ID) values
                    (:BAIRRO, :id_cidade)
                    RETURNING ID into :id_bairro;
            end
        end
        else
        begin
            id_bairro = :BAIRRO_ALT_ID;
        end

        select first 1 r.ID from ENDERECO r where
            lower(r.LOGRADOURO) like '%' || lower(:LOGRADOURO) || '%'
            AND r.NUMERO = :NUMERO
            AND (
                lower(r.COMPLEMENTO) like '%' || lower(:COMPLEMENTO) || '%' OR
                (:complemento is null AND r.COMPLEMENTO is null)
            )
            AND (
                lower(r.REFERENCIA) like '%' || lower(:REFERENCIA) || '%' OR
                (:REFERENCIA is null AND r.REFERENCIA is null)
            )
            AND (
                lower(r.CEP) like '%' || lower(:CEP) || '%' OR
                (:CEP is null AND r.CEP is null)
            )
            AND r.BAIRRO_ID = :id_bairro
            into :ID;

        if (:ID is null) then
        begin
            insert into ENDERECO(LOGRADOURO, NUMERO, COMPLEMENTO, REFERENCIA, CEP, LATITUDE, LONGITUDE, BAIRRO_ID)
                values (:logradouro, :numero, :complemento, :referencia, :cep, :latitude, :longitude, :id_bairro)
                RETURNING ID into :ID;
        end
    end

   SUSPEND;
END^
SET TERM ; ^
comment on procedure PR_REGISTRA_ENDERECO is 'Efetua a identificação das informações e o cadastro do endereço caso ainda não exista na base';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_USUARIO_NOVO (
    ID_DEV bigint,
    HOST_DEV DM_HOST_PORT,
    FACEBOOK_ID varchar(35),
    CPF DM_CPF,
    NOME DM_NOME,
    SOBRENOME DM_SOBRENOME,
    EMAIL DM_EMAIL,
    TELEFONE_NUMERO DM_TELEFONE,
    SENHA varchar(100) )
AS
DECLARE VARIABLE ID bigint;
declare variable ID_ACAO_DEV bigint;
declare variable ID_TEL bigint;
declare variable ID_FACEBOOK_CADASTRADO varchar(35);
BEGIN
    SELECT 
        a.ID, 
        a.FACEBOOK_ID 
    FROM 
        USUARIO a 
    WHERE 
        a.CPF = :CPF or 
        a.EMAIL = :EMAIL 
    INTO :ID, :ID_FACEBOOK_CADASTRADO;
    if (FACEBOOK_ID is not null AND :ID is not null) then
    BEGIN
        if ((:ID_FACEBOOK_CADASTRADO is null OR :ID_FACEBOOK_CADASTRADO != :FACEBOOK_ID)) then
        BEGIN
            UPDATE USUARIO a
            SET 
                a.CPF = :CPF, 
                a.NOME = :nome, 
                a.SOBRENOME = :sobrenome, 
                a.EMAIL = :email, 
                a.FACEBOOK_ID = :facebook_id
            WHERE
                a.ID = :ID;
        END
        
    END
    ELSE
    BEGIN
            
        if (:ID is null)then
        begin
            IN AUTONOMOUS TRANSACTION DO
            BEGIN
                INSERT INTO DISPOSITIVO_ACAO (HORARIO, HOST_CLIENTE, DISPOSITIVO_ID)
                values ('now', :host_dev, :id_dev) RETURNING ID INTO :id_acao_dev;
                INSERT INTO USUARIO (CPF, DISPOSITIVO_ACAO_CADASTRO_ID, EMAIL, NOME, SOBRENOME,
                SENHA, TIPO_USUARIO, FACEBOOK_ID) values (:cpf, :id_acao_dev, :email, :nome, :sobrenome, :senha, 0, :facebook_id)
                RETURNING ID INTO :ID;
                INSERT INTO TELEFONE (NUMERO) VALUES (:TELEFONE_NUMERO) returning ID INTO :id_tel;
            END
            INSERT INTO USUARIO_TELEFONE (USUARIO_ID, TELEFONE_ID) VALUES (:id,:id_tel);
        end
    END
    
END^
SET TERM ; ^
UPDATE RDB$PROCEDURES set
  RDB$DESCRIPTION = 'Efetua o cadastro de um usuário sem a obrigatoriedade de endereço.'
  where RDB$PROCEDURE_NAME = 'PR_REGISTRA_USUARIO_NOVO';
GRANT EXECUTE
 ON PROCEDURE PR_REGISTRA_USUARIO_NOVO TO  SYSDBA;



SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_USUARIO (
    ID_DEV BIGINT,
    HOST_DEV DM_HOST_PORT,
    FACEBOOK_ID VARCHAR(35),
    CPF DM_CPF,
    NOME DM_NOME,
    SOBRENOME DM_SOBRENOME,
    EMAIL DM_EMAIL,
    TELEFONE_NUMERO DM_TELEFONE,
    SENHA VARCHAR(100),
    BAIRRO DM_NOME,
    CIDADE DM_NOME,
    ESTADO DM_NOME,
    PAIS DM_NOME,
    LOGRADOURO DM_DESCRICAO,
    NUMERO INTEGER,
    COMPLEMENTO DM_DESCRICAO,
    REFERENCIA DM_REFERENCIA,
    CEP DM_CEP )
RETURNS (
    ID INTEGER )
AS
declare variable ID_ENDERECO int;
declare variable ID_ACAO_DEV bigint;
declare variable ID_TEL bigint;

BEGIN

    SELECT
        p.ID
    FROM
        PR_REGISTRA_ENDERECO (
            :LOGRADOURO,
            :NUMERO,
            :COMPLEMENTO,
            :REFERENCIA,
            :CEP,
            NULL,
            NULL,
            :BAIRRO,
            NULL,
            :CIDADE,
            NULL,
            :ESTADO,
            NULL,
            :PAIS,
            NULL
        ) p
        INTO :ID_ENDERECO;

    IN AUTONOMOUS TRANSACTION DO
        BEGIN
            INSERT INTO DISPOSITIVO_ACAO (HORARIO, HOST_CLIENTE, DISPOSITIVO_ID)
            values ('now', :host_dev, :id_dev) RETURNING ID INTO :id_acao_dev;

            INSERT INTO USUARIO (CPF, DISPOSITIVO_ACAO_CADASTRO_ID, EMAIL, NOME, SOBRENOME,
            SENHA, TIPO_USUARIO, FACEBOOK_ID) values (:cpf, :id_acao_dev, :email, :nome, :sobrenome, :senha, 0, :facebook_id)
            RETURNING ID INTO :ID;

            INSERT INTO TELEFONE (NUMERO) VALUES (:TELEFONE_NUMERO) returning ID INTO :id_tel;
        END

    INSERT INTO USUARIO_TELEFONE (USUARIO_ID, TELEFONE_ID) VALUES (:id,:id_tel);
    INSERT INTO USUARIO_ENDERECO(endereco_id, usuario_id) values (:id_endereco, :ID);

    SUSPEND;
END^
SET TERM  ; ^
comment on procedure PR_REGISTRA_USUARIO is 'Efetua o cadastro de um usuário';


CREATE VIEW VW_USUARIO_LOGADO (USUARIO_ID, DISPOSITIVO_ID, TIPO_USER)
AS
SELECT
    r.USUARIO_ID,
    da.DISPOSITIVO_ID,
    u.TIPO_USUARIO
FROM
    USUARIO_LOGADO r
inner join DISPOSITIVO_ACAO da
    on da.ID = r.DISPOSITIVO_ACAO_LOGIN_ID
inner join USUARIO u
    ON u.ID = r.USUARIO_ID
where
     r.DISPOSITIVO_ACAO_LOGOFF_ID is null ;

SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_LOGADO (
    USER_ID INTEGER,
    DISPOSITIVO_ID BIGINT,
    HOST_CLIENTE DM_HOST_PORT,
    IS_LOGIN DM_BOOL )
AS
declare variable acao_id bigint;
declare variable USER_ID_outro_logado bigint;
BEGIN

     /**
    * Efetua o processo de login/logoff
    */

    insert into DISPOSITIVO_ACAO(DISPOSITIVO_ID,HOST_CLIENTE,HORARIO)
        values (:DISPOSITIVO_ID, :HOST_CLIENTE, 'now')
        returning ID into :acao_id;

    if (:IS_LOGIN = 1) then
    begin

        --Para garantir marcamos como deslogado quem estiver logado no dispositivo
        FOR
           SELECT
                r.USUARIO_ID
            FROM
                VW_USUARIO_LOGADO r
            where
                r.DISPOSITIVO_ID = :DISPOSITIVO_ID
            into :USER_ID_outro_logado
        do
        begin
            update USUARIO_LOGADO r set DISPOSITIVO_ACAO_LOGOFF_ID = :acao_id
                where DISPOSITIVO_ACAO_LOGOFF_ID is null
                    and r.USUARIO_ID = :USER_ID_outro_logado;
        end

        insert into USUARIO_LOGADO(USUARIO_ID, DISPOSITIVO_ACAO_LOGIN_ID)
            values (:USER_ID,:acao_id);
    end
    else
    begin

        if (:USER_ID is null) then
        begin
            /**
            * Casos de logoff forçado
            */

            SELECT
                first 1
                r.USUARIO_ID
            FROM
                VW_USUARIO_LOGADO r
            where
                r.DISPOSITIVO_ID = :DISPOSITIVO_ID
            into :USER_ID;


            update DISPOSITIVO d set d.FORCE_LOGOFF_PENDENTE = 1 where
                d.id = :DISPOSITIVO_ID;

        end



         update USUARIO_LOGADO set DISPOSITIVO_ACAO_LOGOFF_ID = :acao_id
                where DISPOSITIVO_ACAO_LOGOFF_ID is null
                    and USUARIO_ID = :USER_ID;
    end




END^
SET TERM ; ^
comment on procedure PR_REGISTRA_LOGADO is 'Registra o login/logoff de um usuário';


CREATE VIEW VW_ARQUIVOS_DISPONIVEIS_DEV (APP_PATH, PATH, ID, MD5, DEV_ID)
AS
    SELECT
        ad.APP_PATH,
        ad.PATH,
        ad.ID,
        ad.MD5,
        null as DEV_ID
    FROM
        LOJA_ARQUIVO_DISPONIVEL lad
    inner join ARQUIVO_DISPONIVEL ad
        on ad.ID = lad.ARQUIVO_DISPONIVEL_ID
    inner join LOJA l
        on l.ID = lad.LOJA_ID
    where
        ad.EXISTE = 1
        and l.LOJA_STATUS > 0
        AND (ad.PATH like '%png'
            OR ad.PATH like '%gif'
            OR ad.PATH like '%jpg'
            OR ad.PATH like '%jpeg'
            OR ad.PATH like '%categorias%'
            OR ad.PATH like '%notificacoes%'
        )
    UNION all
    SELECT
        ad.APP_PATH,
        ad.PATH,
        ad.ID,
        ad.MD5,
        dad.DISPOSITIVO_ID as DEV_ID
    FROM
        DISPOSITIVO_ARQUIVO_DISPONIVEL dad
    inner join ARQUIVO_DISPONIVEL ad
        on ad.ID = dad.ARQUIVO_DISPONIVEL_ID
    where
        ad.EXISTE = 1
        AND (
            ad.PATH like '%app.json'
        )
    UNION all
    SELECT
        ad.APP_PATH,
        ad.PATH,
        ad.ID,
        ad.MD5,
        null as DEV_ID
    FROM
        CIDADE_ARQUIVO_DISPONIVEL cad
    inner join ARQUIVO_DISPONIVEL ad
        on ad.ID = cad.ARQUIVO_DISPONIVEL_ID
    where
        ad.EXISTE = 1
        AND (ad.PATH like '%png'
            OR ad.PATH like '%gif'
            OR ad.PATH like '%jpg'
            OR ad.PATH like '%jpeg'
        );

SET TERM ^ ;
CREATE PROCEDURE PR_PREPARA_LISTA_CIDADES (
    ID_DEV BIGINT )
RETURNS (
    R_ID INTEGER,
    R_NOME DM_NOME,
    R_IMAGEM VARCHAR(150),
    R_CONTRATANTES_JSON VARCHAR(3000) )
AS
declare variable ID INTEGER;
declare variable IMAGEM VARCHAR(150);
declare variable NOME DM_NOME;
declare variable IS_TESTER SMALLINT;
BEGIN

    SELECT
        u.TIPO_USUARIO
    FROM
        USUARIO u
    JOIN
        VW_USUARIO_LOGADO ul
            ON u.ID = ul.USUARIO_ID
    WHERE
        ul.DISPOSITIVO_ID = :ID_DEV
    INTO :IS_TESTER;

    IF (:IS_TESTER IS NULL) THEN
        BEGIN
            IS_TESTER = 0;
        END

    for SELECT
            r.ID,
            r.NOME,
            ad.APP_PATH
        FROM
            CIDADE r
        INNER join CIDADE_ARQUIVO_DISPONIVEL cad
            on cad.CIDADE_ID = r.id
        inner join ARQUIVO_DISPONIVEL ad
            on ad.ID = cad.ARQUIVO_DISPONIVEL_ID
        where
            ad.EXISTE = 1
        ORDER BY
            ad.ID ASC
        INTO :ID, :NOME, :IMAGEM
    do
     begin
        R_ID = ID;
        R_NOME = NOME;
        R_IMAGEM = IMAGEM;
        R_CONTRATANTES_JSON = null;
        SELECT
            '["' || list(l.CNPJ,'","') || '"]' AS CONTRATANTES
        FROM
            CIDADE r
        inner join BAIRRO b
            on b.CIDADE_ID = r.id
        inner join ENDERECO e
            on e.BAIRRO_ID = b.ID
        inner join LOJA_ENDERECO le
            on le.ENDERECO_ID = e.ID
        inner join LOJA l
            on l.ID = le.LOJA_ID
        where
            le.STATUS_ENDERECO = 1
            and r.ID = :ID
            and (l.LOJA_STATUS = 1
            or (l.LOJA_STATUS = 2 AND :IS_TESTER = 1))
        into :R_CONTRATANTES_JSON;

        if (R_CONTRATANTES_JSON is not null) then
        begin
            SUSPEND;
        end

    end


END^
SET TERM ; ^
comment on procedure PR_PREPARA_LISTA_CIDADES is 'Prepara e retorna a relação de cidades disponíveis para o dispositivo';

CREATE VIEW VW_ARQUIVO_LOJA_DISPONIVEL (APP_PATH, LOJA_ID,
    ARQUIVO_DISPONIVEL_ID, TIPO, PATH)
AS
select
    ad.APP_PATH,
    lad.LOJA_ID,
    lad.ARQUIVO_DISPONIVEL_ID,
    lad.TIPO,
    ad.PATH
from
    LOJA_ARQUIVO_DISPONIVEL lad
inner join ARQUIVO_DISPONIVEL ad
    on ad.id = lad.ARQUIVO_DISPONIVEL_ID
    and ad.EXISTE = 1;

comment on view VW_ARQUIVO_LOJA_DISPONIVEL is 'Retorna os arquivos da loja que estão disponíveis para download';

CREATE VIEW VW_QTDE_PEDIDO_DEV_LOJA (DISPOSITIVO_ID, LOJA_ID, QTDE)
AS
select
    da.DISPOSITIVO_ID,
    p.LOJA_ENDERECO_LOJA_ID,
    count(1)
FROM
    PEDIDO p
inner join DISPOSITIVO_ACAO da
    on da.id = p.DISPOSITIVO_ACAO_ID
group by
    p.LOJA_ENDERECO_LOJA_ID,
    da.DISPOSITIVO_ID;

comment on view VW_QTDE_PEDIDO_DEV_LOJA is 'Retorna a relação de lojaXdispositivo com a quantidade de pedidos que o mesmo realizou na loja';

SET TERM ^ ;
CREATE PROCEDURE PR_PREPARA_LISTA_LOJAS (
    ID_DEV BIGINT )
RETURNS (
    R_ID DM_CNPJ,
    R_DESTAQUE DM_BOOL,
    R_NOME DM_NOME,
    R_IS_ACESSO_RESTRITO DM_BOOL,
    R_IMAGEM VARCHAR(150),
    R_IMAGEM_FULL VARCHAR(150),
    R_ICONE VARCHAR(150),
    R_ICONE_SECUNDARIO VARCHAR(150),
    R_JSON_INFOS VARCHAR(5000) )
AS
declare variable ID INTEGER;
declare variable CIDADE_ID INTEGER;
declare variable QTDE_PEDIDOS_FEITOS_NO_DEV INTEGER;
declare variable CNPJ DM_CNPJ;
declare variable NOME DM_NOME;
declare variable IMAGEM VARCHAR(150);
declare variable IMAGEM_FULL VARCHAR(150);
declare variable ICONE VARCHAR(150);
declare variable ICONE_SECUNDARIO VARCHAR(150);
declare variable JSON_INFOS VARCHAR(5000);
declare variable IS_TESTER SMALLINT;
BEGIN

    SELECT
        u.TIPO_USUARIO
    FROM
        USUARIO u
    JOIN
        VW_USUARIO_LOGADO ul
            ON u.ID = ul.USUARIO_ID
    WHERE
        ul.DISPOSITIVO_ID = :ID_DEV
    INTO :IS_TESTER;

    IF (:IS_TESTER IS NULL) THEN
        BEGIN
            IS_TESTER = 0;
        END

    FOR SELECT
            r.ID,
            r.CNPJ,
            r.NOME,
            r.CIDADE_ID,
            coalesce(a1.APP_PATH, a2.APP_PATH, a3.APP_PATH, a4.APP_PATH, 'not-found-img') as IMAGEM,
            coalesce(a2.APP_PATH, a1.APP_PATH, a3.APP_PATH, a4.APP_PATH, 'not-found-img') as IMAGEM_FULL,
            coalesce(a3.APP_PATH, a4.APP_PATH, 'not-found-img') as ICONE,
            coalesce(a4.APP_PATH, a3.APP_PATH, 'not-found-img') as ICONE_SECUNDARIO,
            l.JSON_TMP_INFOS,
            coalesce(qpdl.QTDE, 0) as QTDE_PEDIDOS_FEITOS_NO_DEV
        FROM
            VW_INFOS_LOJA r
        inner join LOJA l
            on l.ID = r.id
        left join VW_ARQUIVO_LOJA_DISPONIVEL a1
            on a1.LOJA_ID = r.ID and a1.TIPO = 1
        left join VW_ARQUIVO_LOJA_DISPONIVEL a2
            on a2.LOJA_ID = r.ID and a2.TIPO = 2
        left join VW_ARQUIVO_LOJA_DISPONIVEL a3
            on a3.LOJA_ID = r.ID and a3.TIPO = 3
        left join VW_ARQUIVO_LOJA_DISPONIVEL a4
            on a4.LOJA_ID = r.ID and a4.TIPO = 4
        left join VW_QTDE_PEDIDO_DEV_LOJA qpdl
            on qpdl.LOJA_ID = r.ID and qpdl.DISPOSITIVO_ID = :ID_DEV
           where
            (r.LOJA_STATUS = 1
            or (
                r.LOJA_STATUS = 2
                AND :IS_TESTER = 1
            ))
            and (l.IS_FUNCIONAMENTO_FX = 0 or l.IS_FUNCIONAMENTO_FX is null)
        ORDER BY
            QTDE_PEDIDOS_FEITOS_NO_DEV DESC,
            r.APP_INDICE_APRESENTACAO ASC
        INTO
            :ID, :CNPJ, :NOME, :CIDADE_ID, :IMAGEM, :IMAGEM_FULL,
            :ICONE, :ICONE_SECUNDARIO, :JSON_INFOS, :QTDE_PEDIDOS_FEITOS_NO_DEV
    do
    begin
        R_ID = CNPJ;
        R_DESTAQUE = 0;
        R_IS_ACESSO_RESTRITO = 0;
        R_NOME = NOME;
        R_IMAGEM = IMAGEM;
        R_IMAGEM_FULL = IMAGEM_FULL;
        R_ICONE = ICONE;
        R_ICONE_SECUNDARIO = ICONE_SECUNDARIO;
        R_JSON_INFOS = JSON_INFOS;

        if(:QTDE_PEDIDOS_FEITOS_NO_DEV > 0) then
        begin
            R_DESTAQUE = 1;
            R_IMAGEM = IMAGEM_FULL;
        end
        else
        begin
            if ((
                select
                    count(0)
                FROM
                    CIDADE c
                inner join BAIRRO b
                    on b.CIDADE_ID = c.ID
                inner join ENDERECO e
                    on e.BAIRRO_ID = b.ID
                inner join LOJA_ENDERECO le
                    on le.ENDERECO_ID = e.ID
                inner join LOJA l
                    on l.ID = le.LOJA_ID
                where
                    c.CODIGO_IBGE is not null
                    AND b.CIDADE_ID = c.ID
                    and le.STATUS_ENDERECO = 1
                    and c.ID = :CIDADE_ID
                    and (l.LOJA_STATUS = 1 or (l.LOJA_STATUS = 2 AND :IS_TESTER = 1))
            ) <= 3) then
            begin
                R_DESTAQUE = 1;
                R_IMAGEM = IMAGEM_FULL;
            end
        end


        SUSPEND;
    end


END^
SET TERM ; ^
comment on procedure PR_PREPARA_LISTA_LOJAS is 'Prepara e retorna a relação de LOJAS disponíveis para o dispositivo';





SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_APP_TOKEN_FCM (
    DEV_ID BIGINT,
    HOST DM_HOST_PORT,
    TOKEN VARCHAR(300) )
AS
DECLARE VARIABLE tmp BIGINT;
DECLARE VARIABLE last_token varchar(300);
BEGIN

    select first 1
        r.APP_TOKEN_FCM
    from
        DISPOSITIVO_APP_TOKEN_FCM r
    inner join DISPOSITIVO_ACAO da
        on da.ID = r.DISPOSITIVO_ACAO_ID
        and da.DISPOSITIVO_ID = :DEV_ID
    order by
        da.HORARIO desc
    into
        :last_token;

    if(:last_token is null or :last_token <> :TOKEN) then
    begin

        INSERT into DISPOSITIVO_ACAO (DISPOSITIVO_ID, HOST_CLIENTE, HORARIO)
            values ( :DEV_ID, :HOST, 'now') returning ID into tmp;

        insert into DISPOSITIVO_APP_TOKEN_FCM (DISPOSITIVO_ACAO_ID, APP_TOKEN_FCM)
            values (:tmp, :TOKEN);
    end

END^
SET TERM ; ^
comment on procedure PR_REGISTRA_APP_TOKEN_FCM is 'Registra o novo token fcm do dispositivo';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_APP_VERSAO (
    DEV_ID BIGINT,
    HOST DM_HOST_PORT,
    VERSAO INTEGER )
AS
DECLARE VARIABLE tmp BIGINT;
BEGIN

    INSERT into DISPOSITIVO_ACAO (DISPOSITIVO_ID, HOST_CLIENTE, HORARIO)
        values ( :DEV_ID, :HOST, 'now') returning ID into tmp;

    insert into DISPOSITIVO_APP_VERSAO (DISPOSITIVO_ACAO_ID, APP_VERSAO)
        values (:tmp, :VERSAO);


END^
SET TERM ; ^
comment on procedure PR_REGISTRA_APP_VERSAO is 'Registra a nova versão do dispositivo';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_ARQUIVO_DISPONIVEL (
    MD5 DM_MD5,
    TAMANHO BIGINT,
    EXISTE DM_BOOL,
    IS_NEW DM_BOOL,
    PATH VARCHAR(250),
    APP_PATH VARCHAR(150),
    LOJA_RESP INTEGER,
    DEV_RESP BIGINT,
    CIDADE_RESP INTEGER )
AS
DECLARE VARIABLE ID_ARQUIVO BIGINT;
DECLARE VARIABLE MD5_OLD DM_MD5;
DECLARE VARIABLE TAMANHO_OLD BIGINT;
DECLARE VARIABLE EXISTE_OLD DM_BOOL;
BEGIN


  if (:is_new = 1) THEN
  BEGIN
    INSERT INTO ARQUIVO_DISPONIVEL (MD5, TAMANHO, EXISTE, PATH, APP_PATH,
        HORARIO_CADASTRO) values (:md5, :tamanho, :existe, trim(:path),
            :app_path, 'now') returning ID into :ID_ARQUIVO;


     if (:LOJA_RESP is not null) THEN
     BEGIN
     INSERT INTO LOJA_ARQUIVO_DISPONIVEL (LOJA_ID, ARQUIVO_DISPONIVEL_ID, TIPO)
            VALUES (:LOJA_RESP, :ID_ARQUIVO, 0);
     end

     if (:DEV_RESP is not null) THEN
     BEGIN
     INSERT INTO DISPOSITIVO_ARQUIVO_DISPONIVEL (DISPOSITIVO_ID, ARQUIVO_DISPONIVEL_ID)
            VALUES (:DEV_RESP, :ID_ARQUIVO);
     end

     if (:CIDADE_RESP is not null) THEN
     BEGIN
     INSERT INTO CIDADE_ARQUIVO_DISPONIVEL (CIDADE_ID, ARQUIVO_DISPONIVEL_ID)
            VALUES (:CIDADE_RESP, :ID_ARQUIVO);
     end
  end
  else
  BEGIN
    select first 1 MD5, TAMANHO, EXISTE from ARQUIVO_DISPONIVEL r
        where r.PATH = TRIM(:PATH) into :md5_old, :TAMANHO_old, :EXISTE_old;
    if (EXISTE_OLD <> :EXISTE or MD5_OLD <> :MD5 or
            TAMANHO_OLD <> :TAMANHO) THEN
    begin
        update ARQUIVO_DISPONIVEL r set r.EXISTE = :EXISTE, r.MD5 = :MD5,
            r.TAMANHO = :TAMANHO where r.PATH = TRIM(:PATH);
    end

  end

END^
SET TERM ; ^
comment on procedure PR_REGISTRA_ARQUIVO_DISPONIVEL is 'Cadastra/atualiza registro do arquivo disponível para consumo no app';

SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_DOWNLOAD_ARQUIVO (
    ID_DEV BIGINT,
    PATH_FILE VARCHAR(250),
    HOST_CLIENT DM_HOST_PORT,
    IS_START DM_BOOL )
AS
DECLARE VARIABLE ID_ARQUIVO BIGINT;
    DECLARE VARIABLE ID BIGINT;
    DECLARE VARIABLE ID_NEW_ARQ BIGINT;
    DECLARE VARIABLE ID_ACAO BIGINT;
BEGIN
     IF (:IS_START = 1) THEN
        BEGIN
            INSERT INTO DISPOSITIVO_ACAO (DISPOSITIVO_ID, HOST_CLIENTE, HORARIO) VALUES (:ID_DEV, :HOST_CLIENT, 'NOW') RETURNING ID INTO :ID;

            SELECT first 1 r.ID FROM ARQUIVO_DISPONIVEL r WHERE r.PATH = :PATH_FILE INTO :ID_NEW_ARQ;

            INSERT INTO ARQUIVO_DOWNLOAD (ARQUIVO_DISPONIVEL_ID, DISPOSITIVO_ACAO_ID) VALUES (:ID_NEW_ARQ, :ID);
        END

     ELSE
        BEGIN
            SELECT FIRST 1
                a.ARQUIVO_DISPONIVEL_ID AS ID,
                a.DISPOSITIVO_ACAO_ID AS ID_ACAO
            FROM
                ARQUIVO_DOWNLOAD a
            INNER JOIN
                DISPOSITIVO_ACAO da
                    ON da.ID = a.DISPOSITIVO_ACAO_ID
            INNER JOIN
                ARQUIVO_DISPONIVEL ad
                    ON ad.ID = a.ARQUIVO_DISPONIVEL_ID
            WHERE
                da.DISPOSITIVO_ID = :ID_DEV
                AND TRIM(ad.PATH) = TRIM(:PATH_FILE)
            ORDER BY
                da.HORARIO DESC
            INTO :ID_ARQUIVO, :ID_ACAO;

            IF (:ID_ARQUIVO IS NOT NULL) THEN
                BEGIN
                    UPDATE ARQUIVO_DOWNLOAD SET HORARIO_FIM_DOWNLOAD = 'NOW' WHERE DISPOSITIVO_ACAO_ID = :ID_ACAO;
                END
        END

END^
SET TERM ; ^



SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_LOJA (
    CNPJ DM_CNPJ,
    NOME DM_NOME,
    BAIRRO DM_NOME,
    CIDADE DM_NOME,
    ESTADO DM_NOME,
    PAIS DM_NOME,
    LOGRADOURO DM_DESCRICAO,
    NUMERO INTEGER,
    COMPLEMENTO DM_DESCRICAO,
    REFERENCIA DM_REFERENCIA,
    CEP DM_CEP )
AS
declare variable id_endereco int;
declare variable id int;
BEGIN


    if ( (select count(0) from LOJA r where r.CNPJ = :CNPJ) = 0) then
    begin

        INSERT INTO LOJA (CNPJ, NOME, LOJA_STATUS, APP_INDICE_APRESENTACAO,JSON_TMP_INFOS,LAST_UPDATE_INFOS)
            VALUES (
                :CNPJ,
                :NOME,
                0,
                0,
                '{}',
                'now'
          ) RETURNING ID into :ID;


        SELECT p.ID FROM PR_REGISTRA_ENDERECO (:LOGRADOURO, :NUMERO, :COMPLEMENTO, :REFERENCIA, :CEP, null, null,
             :BAIRRO, null, :CIDADE, null, :ESTADO, null, :PAIS, null) p
             into :id_endereco;


        INSERT INTO LOJA_ENDERECO (ENDERECO_ID, STATUS_ENDERECO, LOJA_ID)VALUES (:id_endereco, 1, :ID);

        INSERT INTO LOJA_BONUS_CONF (LOJA_ID, PERCENTUAL, STATUS_BONUS)VALUES (:ID,2.5,1);



    end



END^
SET TERM; ^
comment on procedure PR_REGISTRA_LOJA is 'Efetua o cadastro de uma loja, caso ainda não exista tendo como base seu cnpj';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI (
    LOJA_RESPONSAVEL_ID INTEGER,
    TITULO VARCHAR(150),
    CONTEUDO dm_texto,
    TIPO DM_NOTIFICACAO_TIPO,
    USUARIO_DESTINADO INTEGER,
    DISPOSITIVO_DESTINADO INTEGER )
RETURNS (
    ID BIGINT )
AS
BEGIN
    /**
    * Cada notificação possui sua procedure particular, pois cada mensagem
    * exige determiandos parâmetros
    */
    insert into NOTIFICACAO (loja_responsavel_id, horario_preparo, titulo,
        conteudo,tipo) values (:loja_responsavel_id,'now',:titulo,:conteudo,:tipo)
        RETURNING ID into :ID;

    if (:usuario_destinado is not null) then
        begin
            insert into NOTIFICACAO_USUARIO(NOTIFICACAO_ID, USUARIO_ID)
                values (:ID, :USUARIO_DESTINADO);
        end
    else

        BEGIN
            insert into NOTIFICACAO_DISPOSITIVO(NOTIFICACAO_ID, DISPOSITIVO_ID)
                values (:ID, :DISPOSITIVO_DESTINADO);
        end

  SUSPEND;
END^
SET TERM ; ^
comment on procedure PR_REGISTRA_NOTI is 'Registra uma notificação para um dispositovo/usuário. Cada notificação possuí a própria procedure!';

CREATE VIEW VW_USUARIO_MONITORAMENTO (USUARIO_ID, DISPOSITIVO_ID)
AS
SELECT
    u.ID,
    ul.DISPOSITIVO_ID
FROM
    USUARIO u
JOIN
    VW_USUARIO_LOGADO ul
        ON ul.USUARIO_ID = u.ID
WHERE
    TIPO_USUARIO = 1;

create view VW_USUARIO_ULTIMO_DISPO_LOGADO (USUARIO_ID, DISPOSITIVO_ID) as
with logado as (
    SELECT
        r.USUARIO_ID,
        max(r.DISPOSITIVO_ACAO_LOGIN_ID) as LOGIN_ID
    FROM
        USUARIO_LOGADO r
    group by
        1
)

SELECT
    r.USUARIO_ID,
    da.DISPOSITIVO_ID
FROM
    logado r
inner join DISPOSITIVO_ACAO da
    on da.ID = r.LOGIN_ID;

comment on view VW_USUARIO_ULTIMO_DISPO_LOGADO is 'Retorna o último dispositivo em que o usuário fez login, mesmo estando ou não logado nele no momento';

SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_REQUISICAO_FCM (
    DEV_ID BIGINT )
RETURNS (
    TOKEN VARCHAR(300) )
AS
DECLARE VARIABLE id_acao BIGINT;
DECLARE VARIABLE id_api BIGINT;
BEGIN

    TOKEN = null;

    select
        first 1
        fcm.APP_TOKEN_FCM,
        fcm.DISPOSITIVO_ACAO_ID
    from
        DISPOSITIVO_APP_TOKEN_FCM fcm
    inner join DISPOSITIVO_ACAO da
        on da.ID = fcm.DISPOSITIVO_ACAO_ID
    where
        da.DISPOSITIVO_ID = :DEV_ID
    order by
        da.HORARIO DESC
    into
        :TOKEN, :id_acao
    ;

    if (:TOKEN is not null) then
    begin

        INSERT into USO_API_EXTERNA (HORARIO_CONSULTA, API_USADA, CONCLUSAO_PENDENTE)
            values ('now', 0, 1) RETURNING ID into :ID_API;

        insert into USO_API_EXTERNA_TOKEN_FCM (USO_API_EXTERNA_ID, DISPOSITIVO_APP_TOKEN_ID)
            VALUES (:ID_API, :id_acao);
        SUSPEND;
    end
END^
SET TERM; ^

comment on procedure PR_REGISTRA_REQUISICAO_FCM is 'Registra que a consula vai ser realizada e retorna o token para a mesma';

SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_B_1 (
    USUARIO_ID BIGINT,
    PEDIDO_ID BIGINT )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    BL DECIMAL(18,4),
    BB DECIMAL(18,4),
    TOKEN VARCHAR(300) )
AS
declare VARIABLE bonus_creditados varchar(23);
declare VARIABLE horario_pedido varchar(19);
BEGIN
   /**
   * Quando for creditado algum bonus ao usuario por causa de um pedido
   */

    select
        (select FORMATADO from PR_FORMAT_HORARIO(da.HORARIO)),
        (select MONEY_REAL from PR_FORMAT_MONEY_REAL((r.TOTAL / 100) * lbc.PERCENTUAL,2))

    FROM
        PEDIDO r
    inner join LOJA_BONUS_CONF lbc
        on lbc.LOJA_ID = r.LOJA_ENDERECO_LOJA_ID
    inner join DISPOSITIVO_ACAO da
        on da.id = r.DISPOSITIVO_ACAO_ID
    where
        lbc.ID = r.LOJA_BONUS_CONF_ID
        and r.id = :PEDIDO_ID
    into
       :horario_pedido, :bonus_creditados;


    SELECT
         p.TOTAL_BONUS_LIVRE,
         p.TOTAL_BONUS_BLOQUEADO
    FROM
        PR_RETORNA_BONUS_USUARIO (:usuario_id) p
    into
        :BL, :BB;

            SELECT
                ID
            FROM
                PR_REGISTRA_NOTI(
                    null,
                    'Bônus Gerado',
                    'e-GULA informa que foram creditados ' || COALESCE(:bonus_creditados,'ERRO!') ||
                    ' de bônus referentes ao pedido #' || COALESCE(:PEDIDO_ID,'ERRO!') ||
                    ' efetuado em ' || COALESCE(:horario_pedido,'ERRO!') ||
                    '. Seu saldo de bônus agora é ' || COALESCE((select MONEY_REAL from PR_FORMAT_MONEY_REAL(:BL, 2)),'ERRO!'),
                    0,
                    :USUARIO_ID,
                    null
                )
            INTO :ID;

   SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;

    SUSPEND;

END^
SET TERM ; ^
comment on procedure PR_REGISTRA_NOTI_B_1 is 'Quando for creditado algum bonus ao usuario por causa de um pedido    ';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_B_2 (
    USUARIO_ID INTEGER )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
BEGIN
   /**
   * Mensagem de negação de pedido por problemas com bônus. Disparada quando alguém
   * tenta comprar algo, pagando em bônus, mas com um valor maior do que seu saldo.
   *
   * É realizado um tratamento no app, mas por garantia....
   */
    SELECT
        ID
    FROM
        PR_REGISTRA_NOTI(
            null,
            null,
            'Desculpe, mas o e-GULA Administrador identificou inconformidades com seu' ||
            ' saldo de bônus, seu pedido foi cancelado.',
            0,
            :usuario_id,
            null
        )
    INTO ID;

    SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;

    SUSPEND;


END^
SET TERM ; ^
comment on procedure PR_REGISTRA_NOTI_B_2 is 'Mensagem de negação de pedido por problemas com bônus. Disparada quando alguém tenta comprar algo, pagando em bônus, mas com um valor maior do que seu saldo.';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_B_3 (
    USUARIO_ID BIGINT,
    PEDIDO_ID BIGINT )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    BL DECIMAL(18,4),
    BB DECIMAL(18,4),
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
declare VARIABLE bonus_livre varchar(23);
declare VARIABLE bonus_creditados varchar(23);
BEGIN
   /**
   * Quando for confirmado um pedido pago com bônus. Nesses casos a
   * PR_REGISTRA_NOTI_B_1 não se aplica.
   */

    select
        (select MONEY_REAL from PR_FORMAT_MONEY_REAL((r.TOTAL / 100) * lbc.PERCENTUAL,2))

    FROM
        PEDIDO r
    inner join LOJA_BONUS_CONF lbc
        on lbc.LOJA_ID = r.LOJA_ENDERECO_LOJA_ID
    where
        lbc.ID = r.LOJA_BONUS_CONF_ID
        and r.id = :PEDIDO_ID
    into   :bonus_creditados;


    SELECT
         p.TOTAL_BONUS_LIVRE,
         p.TOTAL_BONUS_BLOQUEADO
    FROM
        PR_RETORNA_BONUS_USUARIO (:usuario_id) p
    into
        :BL, :BB;

    SELECT
        ID
    FROM
        PR_REGISTRA_NOTI(
            null,
            'Bônus Gerado',
            'Parabéns, seu pedido #' || COALESCE(:PEDIDO_ID,'ERRO!') ||
            ' foi aceito e pago com seus bônus e-GULA.' ||
            ' Seu saldo atual de bônus é de ' || COALESCE((select MONEY_REAL from PR_FORMAT_MONEY_REAL(:BL, 2)),'ERRO!') ||
            ', já creditado ' || COALESCE(:bonus_creditados,'ERRO!') ||
            ' referente ao bônus gerado pelo pedido',
            0,
            :USUARIO_ID,
            null
        )
    INTO :ID;

    SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;

    SUSPEND;

END^
SET TERM; ^
comment on procedure PR_REGISTRA_NOTI_B_3 is 'Quando for confirmado um pedido pago com bônus. Nesses casos a PR_REGISTRA_NOTI_B_1 não se aplica. ';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_C_1 (
    USUARIO_ID INTEGER )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
declare VARIABLE nome DM_NOME;
BEGIN
   /**
   * Mensagem de boas vindas ao novo usuário
   */

    SELECT
        r.NOME
    FROM
        USUARIO r
    where
        r.id = :USUARIO_id
    into
        :nome;

    SELECT
        ID
    FROM
        PR_REGISTRA_NOTI(
            null,
            'Cadastro Concluído!',
            'Parabéns ' || COALESCE(:nome,'ERRO!') ||
            '! Seu processo de cadastro foi concluído em nossa central. ' ||
            'Você é o usuário e-GULA #' || COALESCE(:USUARIO_id,'ERRO!'),
            3,
            :usuario_id,
            null
        )
    INTO :ID;

    SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;
    SUSPEND;
END^
SET TERM ; ^
comment on procedure PR_REGISTRA_NOTI_C_1 is 'Mensagem de boas vindas ao novo usuário ';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_P_1 (
    ID_USUARIO BIGINT,
    PEDIDO_ID BIGINT,
    PREVISAO_ENTREGA TIMESTAMP )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
declare VARIABLE loja_id int;
declare VARIABLE loja_nome varchar(100);
BEGIN
    /**
    * Quando um pedido for confirmado, mas não pago com bônus
    */

    select
        r.LOJA_ENDERECO_LOJA_ID,
        l.NOME
    FROM
        PEDIDO r
    inner join loja l
        on l.id = r.LOJA_ENDERECO_LOJA_ID
    where
        r.id = :PEDIDO_ID
    into
        :loja_id, :loja_nome;

    SELECT
        ID
    FROM
        PR_REGISTRA_NOTI(
            :loja_id,
            'Pedido Confirmado',
            COALESCE(:loja_nome,'ERRO!') ||
            ' agradece a sua preferência. Seu pedido foi confirmado e entregaremos' ||
            ' aproximadamente as ' || COALESCE((select FORMATADO from PR_FORMAT_HORARIO(:PREVISAO_ENTREGA)),'ERRO!'),
            1,
            :ID_USUARIO,
            null
        )
    INTO :ID;

    UPDATE NOTIFICACAO SET
PEDIDO_ID = :PEDIDO_ID
WHERE ID = :ID and PEDIDO_ID IS NULL;

   SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;
    SUSPEND;
END^
SET TERM ; ^
comment on procedure PR_REGISTRA_NOTI_P_1 is 'Quando um pedido for confirmado, mas não pago com bônus  ';



SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_P_2 (
    ID_USUARIO BIGINT,
    PEDIDO_ID BIGINT,
    PREVISAO_ENTREGA TIMESTAMP )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
DECLARE VARIABLE nome_loja dm_nome;
    DECLARE VARIABLE nome_user dm_nome;

BEGIN
   /**
   * Mensagem de controle para dispositivos de monitoramento. Disparado quando um pedido � respondido pela loja.
   */

    select
        l.NOME,
        u.NOME_COMPLETO
    from
        PEDIDO p
    inner join USUARIO u
        on u.ID = p.USUARIO_ENDERECO_USUARIO_ID
    inner join loja l
        on l.ID = p.LOJA_ENDERECO_LOJA_ID
    WHERE
        p.ID = :PEDIDO_ID
    INTO
        :nome_loja, :nome_user;

    SELECT
        p.ID
    FROM
        PR_REGISTRA_NOTI(
            null,
            'Monitoramento e-GULA',
            'Confirmado '
            ||  :nome_user
            || ' #'
            || :PEDIDO_ID
            || ' '
            ||  (SELECT p.FORMATADO FROM PR_FORMAT_HORARIO ('now') p)
            || '<n>Loja: ' || :nome_loja
            || '<n>Previsão de entrega informada pela loja: '
            || (SELECT p.FORMATADO FROM PR_FORMAT_HORARIO(:PREVISAO_ENTREGA) p ),
            3,
            :ID_USUARIO,
            null
        ) p
        INTO :ID;

        SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;
    SUSPEND;
END^
SET TERM ; ^


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_P_3 (
    ID_USUARIO bigint,
    PEDIDO_ID bigint,
    ITENS varchar(3000) )
RETURNS (
    ID bigint,
    CONTEUDO DM_TEXTO,
    TITULO varchar(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP char(14),
    TOKEN varchar(300) )
AS
DECLARE VARIABLE NOME_USUARIO VARCHAR(100);
DECLARE VARIABLE TELEFONE_USUARIO VARCHAR(20);
DECLARE VARIABLE DISPOSITIVO VARCHAR(100);
DECLARE VARIABLE LOJA VARCHAR(100);
DECLARE VARIABLE LOJA_STATUS INTEGER;
DECLARE VARIABLE TOTAL varchar(50);
DECLARE VARIABLE ENDERECO varchar(500);
BEGIN
   /**
   * Mensagem de controle para dispositivos de monitoramento. Disparado quando um pedido é efetuado pelo usuário.
   */
    select
        u.NOME_COMPLETO,
        tel.NUMERO,
        l.NOME,
        l.LOJA_STATUS,
        (SELECT MONEY_REAL FROM PR_FORMAT_MONEY_REAL(p.TOTAL, 2)),
        dd.ID_HARDWARE,
        d.LOGRADOURO || ', ' ||
        d.NUMERO || '<n>' ||
        d.BAIRRO || ', ' ||
        d.CIDADE || '<n>' ||
        coalesce(iif(d.COMPLEMENTO <> '', d.COMPLEMENTO, null) || '. ', '') ||
        coalesce(iif(d.REFERENCIA <> '', d.REFERENCIA, null), '')

    from
        PEDIDO p
    inner join VW_INFOS_ENDERECO d
        on d.ID = p.USUARIO_ENDERECO_ENDERECO_ID
    inner join USUARIO u
        on u.ID = p.USUARIO_ENDERECO_USUARIO_ID
    inner join loja l
        on l.ID = p.LOJA_ENDERECO_LOJA_ID
    inner join DISPOSITIVO_ACAO da
        on da.ID = p.DISPOSITIVO_ACAO_ID
    inner join DISPOSITIVO dd
        on dd.ID = da.DISPOSITIVO_ID
    inner join USUARIO_TELEFONE t
        on t.ID = p.USUARIO_TELEFONE_ID
    inner join TELEFONE tel
        on tel.ID = t.TELEFONE_ID
    where
        p.ID = :PEDIDO_ID
    INTO
        :NOME_USUARIO,
        :TELEFONE_USUARIO,
        :LOJA,
        :LOJA_STATUS,
        :TOTAL,
        :DISPOSITIVO,
        :ENDERECO;

    SELECT
        p.ID
    FROM
        PR_REGISTRA_NOTI(
            null,
            'Monitoramento e-GULA',
            IIF (:LOJA_STATUS = 0, 'LOJA DESATIVADA!!'|| '<n>'|| '<n>', '') ||
            'Emitido: '|| :NOME_USUARIO ||' '|| :TOTAL ||' '|| (SELECT p.FORMATADO FROM PR_FORMAT_HORARIO ('now') p)
                ||'<n>'

                ||'Loja: ' || :LOJA
                ||'<n>'

                ||'Pedido Nº: #' ||:PEDIDO_ID
                ||'<n>'

                ||'Dispositivo: ' || :DISPOSITIVO
                ||'<n>'

                ||'Telefone Usuário: ' || :TELEFONE_USUARIO
                ||'<n>'

                ||'Endereço: ' || :ENDERECO
                ||'<n>'

                ||'Produtos: ' || :ITENS,
            3,
            :ID_USUARIO,
            null
        ) p
        INTO :ID;

        SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;
    SUSPEND;

END^
SET TERM ; ^
comment on procedure PR_REGISTRA_NOTI_P_3 is 'Mensagem de controle para dispositivos de monitoramento. Disparado quando um pedido é efetuado pelo usuário.';


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_P_4 (
    ID_USUARIO BIGINT,
    JSON_BRUTO VARCHAR(5000) )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
BEGIN
        /**
        * Mensagem de controle para dispositivos de monitoramento. Disparado antes de um pedido ser efetuado
        */
        SELECT
            p.ID
        FROM
            PR_REGISTRA_NOTI(
                null,
                'Monitoramento e-GULA',
                'Pré pedido: '|| :JSON_BRUTO,
                3,
                :ID_USUARIO,
                null
            ) p
        INTO
            :ID;

       SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;

        SUSPEND;

    END^
SET TERM ; ^
comment on procedure PR_REGISTRA_NOTI_P_4 is 'Mensagem de controle para dispositivos de monitoramento. Disparado antes de um pedido ser efetuado.';

SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_RESTART (
    ID_USUARIO BIGINT )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
BEGIN
        SELECT
            r.ID
        FROM
            PR_REGISTRA_NOTI(
                null,
                'Monitoramento e-GULA',
                'Capp reiniciará em 15min: ' || (SELECT p.FORMATADO FROM PR_FORMAT_HORARIO ('now') p),
                3,
                :ID_USUARIO,
                NULL
            ) r
        INTO
            :ID;

        SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;

        SUSPEND;
    END^
SET TERM  ; ^


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_NOTI_START (
    ID_USUARIO BIGINT )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
BEGIN
    in autonomous TRANSACTION do
    begin
    SELECT
        r.ID
    FROM
        PR_REGISTRA_NOTI(
            null,
            'Monitoramento e-GULA',
            'Capp iniciado com sucesso: ' || (SELECT p.FORMATADO FROM PR_FORMAT_HORARIO ('now') p),
            3,
            :ID_USUARIO,
            NULL
        ) r
    INTO
        :ID;
    end

        SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;
    SUSPEND;
END^
SET TERM ; ^


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_PEDIDO (
    DESTINO_LOGRADOURO DM_DESCRICAO,
    DESTINO_NUMERO VARCHAR(10),
    DESTINO_COMPLEMENTO DM_DESCRICAO,
    DESTINO_REFERENCIA DM_REFERENCIA,
    DESTINO_CEP DM_CEP,
    DESTINO_BAIRRO DM_NOME,
    DESTINO_CIDADE DM_NOME,
    DESTINO_ESTADO DM_NOME,
    DESTINO_PAIS DM_NOME,
    DESTINO_ENDERECO_ID_ALT INTEGER,
    LOJA_CNPJ DM_CNPJ,
    LOJA_ID_ALT INTEGER,
    AUTOR_DISPOSITIVO_ID BIGINT,
    AUTOR_DISPOSITIVO_HOST_CLIENTE DM_HOST_PORT,
    AUTOR_TELEFONE DM_TELEFONE,
    PEDIDO_TOTAL DM_MONEY,
    PEDIDO_PAGO_EM_BONUS DM_BOOL,
    PEDIDO_JSON_TMP_SC VARCHAR(5000) )
RETURNS (
    ID_PEDIDO BIGINT )
AS
declare VARIABLE loja_endereco_id integer;
declare VARIABLE loja_bonus_id integer;
declare VARIABLE dev_acao_id bigint;
declare VARIABLE tel_user_id bigint;
declare VARIABLE tel_id bigint;
declare VARIABLE AUTOR_USUARIO_ID bigint;

BEGIN
    /**
    * Registra um novo pedido na base para ser posteriormente disponibilizados
    * aos SC''s
    */

    SELECT
        r.ID,
        r.ENDERECO_ID,
        r.PERCENTUAL_ID
    FROM
        VW_INFOS_LOJA r
    where
        (:LOJA_CNPJ is not null AND r.CNPJ = :LOJA_CNPJ)
        or (:LOJA_ID_ALT is not null AND r.ID = :LOJA_ID_ALT)
    into
        :LOJA_ID_ALT, :loja_endereco_id, :loja_bonus_id;

    AUTOR_USUARIO_ID = null;

    select
        r.USUARIO_ID
    FROM
        VW_USUARIO_LOGADO r
    where
        r.DISPOSITIVO_ID = :AUTOR_DISPOSITIVO_ID
    into
        :AUTOR_USUARIO_ID;

    if (:AUTOR_USUARIO_ID is null) then
    begin
        SELECT
            r.ID
        FROM
            USUARIO r
        where
            r.CPF = '11111111111'
        into
            :AUTOR_USUARIO_ID;
    end


    insert into DISPOSITIVO_ACAO (DISPOSITIVO_ID, HOST_CLIENTE,HORARIO)
        values (:AUTOR_DISPOSITIVO_ID,:AUTOR_DISPOSITIVO_HOST_CLIENTE, 'now')
        RETURNING ID into :dev_acao_id;


    if (:DESTINO_ENDERECO_ID_ALT is null) then
    begin
        if (:DESTINO_LOGRADOURO is not null) then
        begin

            IN AUTONOMOUS TRANSACTION DO
            BEGIN
                SELECT
                    p.ID
                FROM
                    PR_REGISTRA_ENDERECO (:DESTINO_LOGRADOURO, :DESTINO_NUMERO,
                        :DESTINO_COMPLEMENTO, :DESTINO_REFERENCIA, :DESTINO_CEP, null,null, :DESTINO_BAIRRO, null,
                        :DESTINO_CIDADE, null, :DESTINO_ESTADO,
                        null, :DESTINO_PAIS, null) p
                into :DESTINO_ENDERECO_ID_ALT;

                 if ((select count(0) from USUARIO_ENDERECO r
                        where r.ENDERECO_ID = :DESTINO_ENDERECO_ID_ALT
                            and r.USUARIO_ID = :AUTOR_USUARIO_ID) = 0) then
                begin

                    insert into USUARIO_ENDERECO(ENDERECO_ID, USUARIO_ID)
                        values (:DESTINO_ENDERECO_ID_ALT,:AUTOR_USUARIO_ID);

                end
            end
        end
        else
        begin

            /**
            * Casos onde o consumo é local. Para esses casos relacionamos o endereço
            * do restaurante com o usuário. Esses enderços não são retornados pela
            * VW_INFOS_USUARIO_ENDERECO
            */

            if ((select count(0) from USUARIO_ENDERECO r
                where r.ENDERECO_ID = :loja_endereco_id
                    and r.USUARIO_ID = :AUTOR_USUARIO_ID) = 0) then
            begin

                insert into USUARIO_ENDERECO(ENDERECO_ID, USUARIO_ID)
                    values (:loja_endereco_id,:AUTOR_USUARIO_ID);

            end

            DESTINO_ENDERECO_ID_ALT = :loja_endereco_id;

        end
    end

    SELECT
        r.ID
    FROM
        VW_INFOS_USUARIO_TELEFONE r
    where
        r.USUARIO_ID = :AUTOR_USUARIO_ID
        and r.NUMERO = :AUTOR_TELEFONE
    into :tel_user_id;

    /**
    * Se o telefone que o usuário passou ainda não pertencer a ele vamos
    * cadastrar
    */
    if (:tel_user_id is null) then
    begin

        INSERT INTO TELEFONE (NUMERO) VALUES (:AUTOR_TELEFONE) returning ID into :tel_id;

        INSERT INTO USUARIO_TELEFONE (USUARIO_ID, TELEFONE_ID) VALUES (:AUTOR_USUARIO_ID,:tel_id)
        returning ID into :tel_user_id;

    end


    INSERT INTO PEDIDO (
        LOJA_BONUS_CONF_ID,
        LOJA_ENDERECO_ENDERECO_ID,
        LOJA_ENDERECO_LOJA_ID,
        USUARIO_ENDERECO_USUARIO_ID,
        USUARIO_ENDERECO_ENDERECO_ID,
        DISPOSITIVO_ACAO_ID,
        TOTAL_TMP,
        JSON_TMP_SC,
        PAGO_EM_BONUS,
        USUARIO_TELEFONE_ID
        )
    VALUES (
        :loja_bonus_id,
        :loja_endereco_id,
        :LOJA_ID_ALT,
        :AUTOR_USUARIO_ID,
        :DESTINO_ENDERECO_ID_ALT,
        :dev_acao_id,
        :PEDIDO_TOTAL,
        :PEDIDO_JSON_TMP_SC,
        :PEDIDO_PAGO_EM_BONUS,
        :tel_user_id
    )
    RETURNING ID INTO :ID_PEDIDO;

  SUSPEND;
END^
SET TERM  ; ^

comment on procedure PR_REGISTRA_PEDIDO is 'Efetua o registro de um pedido';



SET TERM ^ ;
CREATE PROCEDURE PR_RETORNA_CADASTRO_MONITOR (
    LOJA_ID INTEGER,
    MAC_MG DM_MAC )
RETURNS (
    ID INTEGER )
AS
BEGIN

    ID = null;

    select
        m.ID
    from
        MONITOR_SC m
    where
        m.LOJA_ID = :LOJA_ID AND
        m.MAC = :MAC_MG
    into :ID;

    if (:ID is null) then
    begin
        IN AUTONOMOUS TRANSACTION DO
        BEGIN
            insert into MONITOR_SC(LOJA_ID, MAC) values (:LOJA_ID,:MAC_MG)
            RETURNING ID into :ID;
        end
    end

    SUSPEND;

END^
SET TERM ; ^
comment on procedure PR_RETORNA_CADASTRO_MONITOR is 'Cadastra, caso não exista, e retorna o id do monitor com base no seu mac+loja';

SET TERM ^ ;
CREATE PROCEDURE PR_RETORNA_IMAGEM_COMUM (
    PATH_ORIGINAL VARCHAR(150) )
RETURNS (
    PATH_COMUM VARCHAR(150),
    ID BIGINT )
AS
DECLARE VARIABLE MD5 VARCHAR(32);
BEGIN
    SELECT
        ad.MD5
    FROM
        ARQUIVO_DISPONIVEL ad
    WHERE
        ad.APP_PATH = :PATH_ORIGINAL
    INTO :MD5;

    SELECT FIRST 1
        ad.APP_PATH, ad.ID
    FROM
        ARQUIVO_DISPONIVEL ad
    WHERE
        ad.MD5 = :MD5
        AND ad.EXISTE = 1
    ORDER BY
        ad.ID ASC
    INTO :PATH_COMUM, :ID;

    SUSPEND;

END^
SET TERM; ^




SET TERM ^ ;
CREATE PROCEDURE PR_UP_DEV_INFOS_EXTRA (
    ID_DEV BIGINT,
    MODELO VARCHAR(100),
    FABRICANTE VARCHAR(100),
    TELEFONE_NUMERO DM_TELEFONE,
    TELEFONE_OPERADORA VARCHAR(50),
    SO_VERSAO VARCHAR(10),
    SDK_VERSAO VARCHAR(30) )
AS
DECLARE VARIABLE MODELO_OLD VARCHAR(100);
DECLARE VARIABLE FABRICANTE_OLD VARCHAR(100);
DECLARE VARIABLE TELEFONE_ID bigint;
DECLARE VARIABLE SO_VERSAO_OLD VARCHAR(10);
DECLARE VARIABLE SDK_VERSAO_OLD VARCHAR(30);
BEGIN

   /* select
        COALESCE(:FABRICANTE, d.FABRICANTE),
        COALESCE(:MODELO, d.MODELO),
        d.TELEFONE_ID,
        COALESCE(:SO_VERSAO, d.SO_VERSAO),
        COALESCE(:SDK_VERSAO, d.SDK_VERSAO)
    from
        DISPOSITIVO d
    where
        d.ID = :id_dev
    into
        :FABRICANTE_OLD,
        :MODELO_OLD,
        :TELEFONE_ID,
        :SO_VERSAO_OLD,
        :SDK_VERSAO_OLD;

    if(:TELEFONE_ID is null) then
    begin

        if(:TELEFONE_NUMERO is null) then
        begin
            TELEFONE_NUMERO = '00000000000';
        end

        INSERT INTO TELEFONE (NUMERO,OPERADORA) VALUES (:TELEFONE_NUMERO,:TELEFONE_OPERADORA) returning ID into :TELEFONE_ID;
    end

    UPDATE DISPOSITIVO d
        set d.FABRICANTE = :FABRICANTE_OLD, d.MODELO = :MODELO_OLD,
            d.TELEFONE_ID = :TELEFONE_ID,
            d.SO_VERSAO = :SO_VERSAO_OLD, d.SDK_VERSAO = :SDK_VERSAO_OLD
        where
            d.ID = :id_dev;*/

END^
SET TERM ; ^
comment on procedure PR_UP_DEV_INFOS_EXTRA is 'Efetua a atualização dos demais conteúdos do cadastro do dispositivo como fabricante e afins';


SET TERM ^ ;
CREATE PROCEDURE PR_UP_STATUS_NOTIFS (
    IDS_NOTIS VARCHAR(3000),
    IS_VISUALI DM_BOOL,
    ID_USER BIGINT,
    HOST_PORT_CLIENTE DM_HOST_PORT,
    ID_DEV BIGINT )
AS
DECLARE VARIABLE id_acao bigint;
DECLARE VARIABLE ID_NOTI bigint;
BEGIN

    insert into DISPOSITIVO_ACAO(HORARIO, DISPOSITIVO_ID, HOST_CLIENTE)
        values ('now', :id_dev, :host_port_cliente) RETURNING ID into :id_acao;



    /**
    * Marcamos que o ultimo fcm disparado teve efeito, ja que o app voltou a comunicar
    */
     update USO_API_EXTERNA r set CONCLUSAO_PENDENTE = 0
    where CONCLUSAO_PENDENTE = 1 and
        EXISTS(
            select  1
            from
                USO_API_EXTERNA_TOKEN_FCM fcm
            inner join DISPOSITIVO_ACAO da
                on da.ID = fcm.DISPOSITIVO_APP_TOKEN_ID
            where
                fcm.USO_API_EXTERNA_ID = r.ID
                and da.DISPOSITIVO_ID = :id_dev
    );



    /**
    * Como um notificação é única o update nas duas tabelas pode correr sem problemas
    */
    for
        SELECT
            CAST(p.PART as BIGINT)
        FROM
            PR_SPLIT_STRING (:IDS_NOTIS, ',') p
        INTO
            :ID_NOTI
    do
    begin
        if (:is_visuali = 1) then
        begin

            -- Caso a notificação esteja sendo visualizada e o capp não tenha a confirmação de recebimento da mesma pelo dev
            --Usuário
            update NOTIFICACAO_USUARIO r set r.D_A_RECEBIMENTO_ID = :id_acao
                where r.D_A_RECEBIMENTO_ID is null
                    and r.NOTIFICACAO_ID = :id_noti
                    and r.USUARIO_ID = :id_user;
            -- Caso a notificação esteja sendo visualizada e o capp não tenha a confirmação de recebimento da mesma pelo dev
            --Dispositivo
            update NOTIFICACAO_DISPOSITIVO r set r.D_A_RECEBIMENTO_ID = :id_acao
                where r.D_A_RECEBIMENTO_ID is null
                    and r.NOTIFICACAO_ID = :id_noti
                    and r.DISPOSITIVO_ID = :id_dev;


            --Usuário
            update NOTIFICACAO_USUARIO r set r.D_A_VISUALIZACAO_ID = :id_acao
                where r.D_A_VISUALIZACAO_ID is null
                    and r.NOTIFICACAO_ID = :id_noti
                    and r.USUARIO_ID = :id_user;
            --Dispositivo
            update NOTIFICACAO_DISPOSITIVO r set r.D_A_VISUALIZACAO_ID = :id_acao
                where r.D_A_VISUALIZACAO_ID is null
                    and r.NOTIFICACAO_ID = :id_noti
                    and r.DISPOSITIVO_ID = :id_dev;

        end
        ELSE
        BEGIN
            --Usuário
            update NOTIFICACAO_USUARIO r set r.D_A_RECEBIMENTO_ID = :id_acao
                where r.D_A_RECEBIMENTO_ID is null
                    and r.NOTIFICACAO_ID = :id_noti
                    and r.USUARIO_ID = :id_user;
            --Dispositivo
            update NOTIFICACAO_DISPOSITIVO r set r.D_A_RECEBIMENTO_ID = :id_acao
                where r.D_A_RECEBIMENTO_ID is null
                    and r.NOTIFICACAO_ID = :id_noti
                    and r.DISPOSITIVO_ID = :id_dev;
        end
    end

END^
SET TERM ; ^
comment on procedure PR_UP_STATUS_NOTIFS is 'Atualiza o status da notificação, se a mesma foi visualizada ou recebida, sendo elas de usuário ou dispositivo';


SET TERM ^ ;
CREATE PROCEDURE PR_VALIDA_LOGIN (
    EMAIL DM_EMAIL,
    SENHA VARCHAR(100),
    FACEBOOK_ID VARCHAR(35),
    DEV_ID_ATUAL BIGINT )
RETURNS (
    USER_ID INTEGER,
    LOGADO_EM_OUTRO BIGINT )
AS
BEGIN

    /**
    * Valida a existencia do usuario. Também valida se é
    * possivel o usuário fazer login, como por exemplo os
    * casos do usuário estar logado em outro dispositivo.
    *
    * Estando logado em outro dispositivo não é permitido o
    * login.
    *
    */
    LOGADO_EM_OUTRO = null;
    USER_ID = null;

    SELECT
        u.ID
    FROM
        USUARIO u
    WHERE (
            (
                u.SENHA = :SENHA
                AND (
                        u.EMAIL = :EMAIL
                        OR u.CPF = SUBSTRING(:EMAIL from 1 for 11)
                    )
            )
            OR (
                u.FACEBOOK_ID = :FACEBOOK_ID
            )
    )
    INTO :USER_ID;


    IF (:USER_ID IS NOT NULL) THEN
        BEGIN

            select
                da.DISPOSITIVO_ID
            from
                USUARIO_LOGADO ul
            join
                DISPOSITIVO_ACAO da
                    on da.ID = ul.DISPOSITIVO_ACAO_LOGIN_ID
            where
                ul.USUARIO_ID = :USER_ID
                and ul.DISPOSITIVO_ACAO_LOGOFF_ID is null
            into :LOGADO_EM_OUTRO;

            if (:LOGADO_EM_OUTRO is not null AND :LOGADO_EM_OUTRO <> DEV_ID_ATUAL) then USER_ID = null;

    END

    SUSPEND;

END^
SET TERM  ; ^
comment on procedure PR_VALIDA_LOGIN is 'Efetua a validação do login';








CREATE VIEW VWR_DISPOSITIVOS (ID, ID_HARDWARE, APP_VERSAO, HORARIO_CADASTRO,
    TELEFONE_NUMERO, TELEFONE_OPERADORA, HORARIO_ULTIMA_ACAO, CEL, SO, USR,
    HOST_ULTIMA_ACAO)
AS
      select
    a.ID,
    a.ID_HARDWARE,
    coalesce (a.APP_VERSAO,'')as APP_VERSAO,
    a.HORARIO_CADASTRO,
    coalesce (a.TELEFONE_NUMERO,'')as TELEFONE_NUMERO,
    coalesce (a.TELEFONE_OPERADORA,'')as TELEFONE_OPERADORA,
    coalesce (a.HORARIO_ULTIMA_ACAO,'')as HORARIO_ULTIMA_ACAO,
    coalesce (a.MODELO ||' - '|| a.FABRICANTE,'')as CEL,
    iif( a.SO = '0', 'Android', 'IOS' ) ||coalesce (' - '|| a.SO_VERSAO,'')as SO,
    coalesce ( b.ID ||' - '|| b.NOME ||' '|| b.SOBRENOME,'Sem usuário logado')as USR,
    coalesce ((select
            first 1
            da.HOST_CLIENTE
        from
            DISPOSITIVO_ACAO da
        where
            da.DISPOSITIVO_ID = a.ID
        order by
            da.id desc
    ),'') as HOST_ULTIMA_ACAO

        from  VW_INFOS_DISPOSITIVO a
    left Join VW_USUARIO_LOGADO c on a.ID = c.DISPOSITIVO_ID
    left Join VW_INFOS_USUARIO b on b.ID = c.USUARIO_ID;
CREATE VIEW VWR_LOJA_SALDOS_PEDIDOS (ID, NOME, CNPJ, SALDO_PEDIDOS_LIBERADOS)
AS
SELECT
    r.ID,
    r.NOME,
    r.CNPJ,
    (
        coalesce((
            select sum(c.CREDITO)
            from LOJA_CREDITO c
            where c.LOJA_ID = r.ID
        ),0)
        -
        coalesce((
            select sum(p.TOTAL)
            from PEDIDO p
            INNER join LOJA_BONUS_CONF lb
                on lb.ID = p.LOJA_BONUS_CONF_ID
            where lb.LOJA_ID = r.ID
                and p.IS_CANCELADO = 0
        ),0)
    ) AS SALDO_PEDIDOS_LIBERADOS
FROM
    LOJA r;


CREATE VIEW VWR_USUARIOS (NOME, CODIGO, CPF, BONUS, CADASTRO, EMAIL, TELEFONE,
    CIDADE)
AS
    SELECT
    (u.NOME ||' '|| u.SOBRENOME) AS NOME,
    u.ID AS CODIGO,
    u.CPF,
    /*BONUS*/
    bu.TOTAL_BONUS_LIVRE as BONUS,
    /*CADASTRO*/
    ((SELECT FORMATADO
     FROM PR_FORMAT_HORARIO (dac.HORARIO))) AS CADASTRO,
    u.EMAIL,
    /*TELEFONE*/
    (SELECT first 1
         r.NUMERO
     FROM
         VW_INFOS_USUARIO_TELEFONE r
     where
         r.USUARIO_ID = u.ID
     order by
         r.TELEFONE_ID desc) AS TELEFONE,
    /*CIDADE*/
    coalesce((SELECT first 1
         r.CIDADE
     FROM
         VW_INFOS_USUARIO_ENDERECO r
     where
         r.USUARIO_ID = u.id
     order by
         r.ID desc), 'Não encontrada') as CIDADE


FROM
    USUARIO u
JOIN
    DISPOSITIVO_ACAO dac ON dac.ID = u.DISPOSITIVO_ACAO_CADASTRO_ID
JOIN
    DISPOSITIVO dc ON dc.ID = dac.DISPOSITIVO_ID
left join PR_RETORNA_BONUS_USUARIO(u.ID) bu
        on 1 = 1;




CREATE VIEW VW_ARQUIVO_INICIAL_DISPONIVEL (DISPOSITIVO_ID,
    ARQUIVO_DISPONIVEL_ID, PATH, NAME, NAME_TO)
AS
    SELECT DISTINCT
        dad.DISPOSITIVO_ID,
        dad.ARQUIVO_DISPONIVEL_ID,
        ad.PATH,
        ad.APP_PATH AS NAME,
        TRIM(iif(ad.APP_PATH CONTAINING 'lista-contratantes' OR
            ad.APP_PATH CONTAINING 'regiao-cidades',
                iif(ad.APP_PATH CONTAINING 'regiao-cidades','regiao-cidades',
                    iif(ad.APP_PATH CONTAINING 'lista-contratantes', 'contratantes',null))
        ,null)) AS NAME_TO
    FROM
        DISPOSITIVO_ARQUIVO_DISPONIVEL dad
    JOIN
        ARQUIVO_DISPONIVEL ad
            ON ad.ID = dad.ARQUIVO_DISPONIVEL_ID
    JOIN
        ARQUIVO_DOWNLOAD a
            ON a.ARQUIVO_DISPONIVEL_ID = ad.ID and a.HORARIO_FIM_DOWNLOAD IS NULL
    WHERE
        ad.EXISTE = 1
        AND ad.APP_PATH NOT LIKE '%log%'
UNION
    SELECT DISTINCT
        dad.DISPOSITIVO_ID,
        dad.ARQUIVO_DISPONIVEL_ID,
        ad.PATH,
        ad.APP_PATH AS NAME,
        TRIM(iif(ad.APP_PATH CONTAINING 'lista-contratantes' OR
            ad.APP_PATH CONTAINING 'regiao-cidades',
                iif(ad.APP_PATH CONTAINING 'regiao-cidades','regiao-cidades',
                    iif(ad.APP_PATH CONTAINING 'lista-contratantes', 'contratantes',null))
        ,null)) AS NAME_TO
    FROM
        DISPOSITIVO_ARQUIVO_DISPONIVEL dad
    JOIN
        ARQUIVO_DISPONIVEL ad
            ON ad.ID = dad.ARQUIVO_DISPONIVEL_ID
    WHERE
        ad.EXISTE = 1
        AND ad.APP_PATH NOT LIKE '%log%'
        AND NOT EXISTS(
                    SELECT
                        a.ARQUIVO_DISPONIVEL_ID
                    FROM
                        ARQUIVO_DOWNLOAD a
                    WHERE
                        a.ARQUIVO_DISPONIVEL_ID = ad.ID
                        )
;


CREATE VIEW VW_INFOS_CIDADE (ID, NOME, CODIGO_IBGE, ESTADO, ESTADO_ID, PAIS,
    PAIS_ID)
AS
SELECT
    r.ID,
    r.NOME,
    r.CODIGO_IBGE,
    uf.SIGLA as estado,
    uf.ID as estado_id,
    p.NOME as pais,
    p.id as pais_id
FROM
    CIDADE r
inner join ESTADO uf
    on uf.ID = r.ESTADO_ID
inner join PAIS p
    on p.ID = uf.PAIS_ID;

CREATE VIEW VW_INFOS_NOTI_DISPOSITIVO (ID, DISPOSITIVO_ID, IS_VISUALIZADA,
    IS_RECEBIDA, IS_FROM_CAPP, D_A_RECEBIMENTO_ID, D_A_VISUALIZACAO_ID, TITULO,
    CONTEUDO, TIPO, HORARIO_PREPARO, LOJA_RESPONSAVEL_ID,
    LOJA_RESPONSAVEL_CNPJ, LOJA_RESPONSAVEL_NOME)
AS
SELECT
    n.ID,
    r.DISPOSITIVO_ID,
    iif(r.D_A_VISUALIZACAO_ID is not null,1,0) AS IS_VISUALIZADA,
    iif(r.D_A_RECEBIMENTO_ID is not null,1,0) AS IS_RECEBIDA,
    iif(n.LOJA_RESPONSAVEL_ID is null,1,0) AS IS_FROM_CAPP,
    r.D_A_RECEBIMENTO_ID,
    r.D_A_VISUALIZACAO_ID,
    n.TITULO,
    n.CONTEUDO,
    n.TIPO,
    n.HORARIO_PREPARO,
    n.LOJA_RESPONSAVEL_ID,
    l.CNPJ AS LOJA_RESPONSAVEL_CNPJ,
    l.NOME AS LOJA_RESPONSAVEL_NOME
FROM
    NOTIFICACAO_DISPOSITIVO r
inner join NOTIFICACAO n
    on n.ID = r.NOTIFICACAO_ID
left join LOJA l
    on l.ID = n.LOJA_RESPONSAVEL_ID;
CREATE VIEW VW_INFOS_NOTI_USUARIO (ID, USUARIO_ID, IS_VISUALIZADA, IS_RECEBIDA,
    IS_FROM_CAPP, D_A_RECEBIMENTO_ID, D_A_VISUALIZACAO_ID, TITULO, CONTEUDO,
    TIPO, HORARIO_PREPARO, LOJA_RESPONSAVEL_ID, LOJA_RESPONSAVEL_CNPJ,
    LOJA_RESPONSAVEL_NOME)
AS
SELECT
    n.ID,
    r.USUARIO_ID,
    iif(r.D_A_VISUALIZACAO_ID is not null,1,0) AS IS_VISUALIZADA,
    iif(r.D_A_RECEBIMENTO_ID is not null,1,0) AS IS_RECEBIDA,
    iif(n.LOJA_RESPONSAVEL_ID is null,1,0) AS IS_FROM_CAPP,
    r.D_A_RECEBIMENTO_ID,
    r.D_A_VISUALIZACAO_ID,
    n.TITULO,
    n.CONTEUDO,
    n.TIPO,
    n.HORARIO_PREPARO,
    n.LOJA_RESPONSAVEL_ID,
    l.CNPJ AS LOJA_RESPONSAVEL_CNPJ,
    l.NOME AS LOJA_RESPONSAVEL_NOME
FROM
    NOTIFICACAO_USUARIO r
inner join NOTIFICACAO n
    on n.ID = r.NOTIFICACAO_ID
left join LOJA l
    on l.ID = n.LOJA_RESPONSAVEL_ID;


CREATE VIEW VW_INFOS_PEDIDOS_PENDENTES (ID, USUARIO_ID, USUARIO_NOME,
    USUARIO_CPF, USUARIO_TELEFONE_ID, USUARIO_TELEFONE_NUMERO,
    DESTINO_ENDERECO_ID, DESTINO_ENDERECO_LOGRADOURO, DESTINO_ENDERECO_NUMERO,
    DESTINO_ENDERECO_COMPLEMENTO,DESTINO_ENDERECO_REFERENCIA, DESTINO_ENDERECO_CEP, DESTINO_PAIS_NOME,
    DESTINO_ESTADO_NOME, DESTINO_ESTADO_SIGLA, DESTINO_CIDADE_NOME,
    DESTINO_CIDADE_IBGE, DESTINO_BAIRRO_NOME, TOTAL, HORARIO_ABERTURA, LOJA_ID,
    JSON_TMP_SC)
AS

select
    p.ID,
    p.USUARIO_ENDERECO_USUARIO_ID as USUARIO_id,
    coalesce(u.NOME || ' ' || u.SOBRENOME, u.NOME) as usuario_nome,
    u.CPF as usuario_cpf,
    p.USUARIO_TELEFONE_ID as USUARIO_TELEFONE_id,
    ut.NUMERO as USUARIO_TELEFONE_numero,
    p.USUARIO_ENDERECO_ENDERECO_ID as destino_endereco_id,
    ed.LOGRADOURO as destino_endereco_logradouro,
    ed.NUMERO as destino_endereco_numero,
    ed.COMPLEMENTO as destino_endereco_complemento,
    ed.referencia as DESTINO_ENDERECO_REFERENCIA,
    ed.CEP as destino_endereco_cep,
    ed.PAIS as destino_pais_nome,
    uf.NOME as destino_estado_nome,
    uf.SIGLA as destino_estado_sigla,
    ed.CIDADE as destino_cidade_nome,
    ci.CODIGO_IBGE as destino_cidade_ibge,
    ed.BAIRRO as destino_bairro_nome,
    p.TOTAL,
    da.HORARIO as horario_abertura,
    p.LOJA_ENDERECO_LOJA_ID as loja_id,
    p.JSON_TMP_SC
from PEDIDO p
inner join DISPOSITIVO_ACAO da
    on da.id = p.DISPOSITIVO_ACAO_ID
LEFT join USUARIO u
    on u.id = p.USUARIO_ENDERECO_USUARIO_ID and not EXISTS(
        select 1 from PEDIDO pp where pp.USUARIO_ENDERECO_USUARIO_ID = u.ID and pp.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null
    )
left join VW_INFOS_USUARIO_TELEFONE ut
    on ut.ID = p.USUARIO_TELEFONE_ID  and not EXISTS(
        select 1 from PEDIDO pp where pp.USUARIO_TELEFONE_ID = ut.ID and pp.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null
    )
left join VW_INFOS_ENDERECO ed
    on ed.ID = p.USUARIO_ENDERECO_ENDERECO_ID and not EXISTS(
        select 1 from PEDIDO pp where pp.USUARIO_ENDERECO_ENDERECO_ID = ed.ID and pp.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null
    )
left join estado uf
    on uf.ID = ed.ESTADO_ID
left join CIDADE ci
    on ci.ID = ed.CIDADE_ID
inner join LOJA l
    on l.ID = p.LOJA_ENDERECO_LOJA_ID
where
    p.MONITOR_SC_COLETOR_ACAO_ID is null
    and p.MONITOR_SC_FALHA_ACAO_ID is null
    and l.IS_FUNCIONAMENTO_FX = 1
    and p.IS_CANCELADO = 0
    and p.TOTAL_TMP is not null
order by da.HORARIO asc ;



create view VW_INFOS_PEDIDOS_PENDENTES_OLD (ID, USUARIO_NOME, TOTAL,
    HORARIO_ABERTURA, CPF, ENVIA_CPF, LOJA_ID, ORIGEM_DISPOSITIVO_HARDWARE_ID,
    DESTINO_TELEFONE, DESTINO_LOGRADOURO, DESTINO_NUMERO, DESTINO_BAIRRO,
    DESTINO_CIDADE, DESTINO_REFERENCIA, DESTINO_COMPLEMENTO, JSON_TMP_SC)
as

select
    p.ID,
    coalesce(u.NOME || ' ' || u.SOBRENOME, u.NOME),
    p.TOTAL,
    da.HORARIO,
    u.CPF,
    coalesce(l.ENVIA_CPF, 0),
    p.LOJA_ENDERECO_LOJA_ID,
    d.ID_HARDWARE,
    iif(CHAR_LENGTH(ut.NUMERO) > 11, substring(ut.NUMERO from CHAR_LENGTH(ut.NUMERO) - 10 for CHAR_LENGTH(ut.NUMERO)), ut.NUMERO) as NUMERO,--gambi até segunda
    substring(ed.LOGRADOURO from 1 for 50) as DESTINO_LOGRADOURO,--gambi até segunda
    substring(ed.NUMERO from 1 for 5) as DESTINO_NUMERO,--gambi até segunda
    substring(ed.BAIRRO from 1 for 40) as DESTINO_BAIRRO,--gambi até segunda
    substring(ed.CIDADE from 1 for 40) as DESTINO_CIDADE,--gambi até segunda
    ed.REFERENCIA as DESTINO_REFERENCIA,
    ed.COMPLEMENTO as DESTINO_COMPLEMENTO,
    p.JSON_TMP_SC
from PEDIDO p
inner join DISPOSITIVO_ACAO da
    on da.id = p.DISPOSITIVO_ACAO_ID
inner join USUARIO u
    on u.id = p.USUARIO_ENDERECO_USUARIO_ID
inner join VW_INFOS_USUARIO_TELEFONE ut
    on ut.ID = p.USUARIO_TELEFONE_ID
inner join VW_INFOS_ENDERECO ed
    on ed.ID = p.USUARIO_ENDERECO_ENDERECO_ID
inner join DISPOSITIVO d
    on d.ID = da.DISPOSITIVO_ID
inner join LOJA l
    on l.ID = p.LOJA_ENDERECO_LOJA_ID
where
    p.MONITOR_SC_COLETOR_ACAO_ID is null
    and (l.IS_FUNCIONAMENTO_FX is null or l.IS_FUNCIONAMENTO_FX = 0)
    and p.IS_CANCELADO = 0
    and p.TOTAL is not null
order by da.HORARIO asc ;


CREATE VIEW VW_INFOS_CREDITO_LOJA_PENDENTE (ID, LOJA_ID, DATA_CADASTRO,
    TOTAL_PEDIDOS_CREDITADO)
AS
SELECT
    r.ID,
    r.LOJA_ID,
    r.DATA_CADASTRO,
    r.TOTAL_PEDIDOS_CREDITADO
FROM
    LOJA_CREDITO r
where
    r.MONITOR_SC_ACAO_ID is null
;

comment on view VWR_LOJA_SALDOS_PEDIDOS is 'Retorna o saldo de pedidos liberados para as lojas';
comment on view VW_ARQUIVOS_DISPONIVEIS_DEV is 'Arquivos disponíveis para consumo no dispositivo';
comment on view VW_INFOS_CIDADE is 'Todas as cidades';
comment on view VW_INFOS_CREDITO_LOJA_PENDENTE is 'Retorna os créditos pendentes de serem distribuídos ao sc';
comment on view VW_INFOS_USUARIO_TELEFONE is 'Informações sobre os telefones do usuário';




CREATE GENERATOR GEN_DISTANCIA_ENTREGA_ID;
SET GENERATOR GEN_DISTANCIA_ENTREGA_ID TO 0;

SET TERM ^ ;
CREATE TRIGGER DISTANCIA_ENTREGA_BI FOR DISTANCIA_ENTREGA
ACTIVE BEFORE INSERT POSITION 0
AS

DECLARE VARIABLE maximo DECIMAL(3,1);

BEGIN

    for select de.DISTANCIA_MAX from DISTANCIA_ENTREGA de where de.LOJA_ID = NEW.LOJA_ID
    into :maximo
        do
        begin
            if (:maximo >= NEW.DISTANCIA_MIN OR NEW.DISTANCIA_MIN > NEW.DISTANCIA_MAX) then exception EXCEP_MAX_EXIST_MAIOR_NOVO_MIN;
        end

    NEW.ID = GEN_ID(GEN_DISTANCIA_ENTREGA_ID, 1);

END^
SET TERM ; ^


SET TERM ^ ;
CREATE TRIGGER DISTANCIA_ENTREGA_BU FOR DISTANCIA_ENTREGA
ACTIVE BEFORE UPDATE POSITION 0
AS

DECLARE VARIABLE maximo DECIMAL(3,1);

BEGIN

    for select de.DISTANCIA_MAX from DISTANCIA_ENTREGA de where de.LOJA_ID = NEW.LOJA_ID
    into :maximo
        do
        begin
            if (:maximo >= NEW.DISTANCIA_MIN OR NEW.DISTANCIA_MIN > NEW.DISTANCIA_MAX) then exception EXCEP_MAX_EXIST_MAIOR_NOVO_MIN;
        end

END^
SET TERM ; ^



SET TERM ^ ;
CREATE TRIGGER ARQUIVO_DISPONIVEL_BIU FOR ARQUIVO_DISPONIVEL ACTIVE
BEFORE INSERT OR UPDATE POSITION 1
AS
BEGIN
   new.PATH = TRIM(new.PATH);
END^
SET TERM ; ^
SET TERM ^ ;
CREATE TRIGGER LOJA_ARQUIVO_DISPONIVEL_AIU FOR LOJA_ARQUIVO_DISPONIVEL ACTIVE
AFTER INSERT OR UPDATE POSITION 1
AS
BEGIN

    /**
    * Tratamento para manter apenas uma imagem como 1,2,3 ou 4 por vez por loja
    */
    if (new.TIPO > 0 AND new.TIPO <> old.TIPO) then
    begin

        update LOJA_ARQUIVO_DISPONIVEL lad set lad.TIPO = 0
            where lad.LOJA_ID = new.LOJA_ID
                and lad.TIPO = new.TIPO
                and lad.ARQUIVO_DISPONIVEL_ID <> new.ARQUIVO_DISPONIVEL_ID;

    end

END^
SET TERM ; ^

SET TERM ^ ;
CREATE PROCEDURE PR_CONFIRMA_FALHA_PEDIDO (
    IDS VARCHAR(500),
    MG_HOST_CLIENTE DM_HOST_PORT,
    MG_SC_ID INTEGER )
AS
DECLARE VARIABLE acao_id BIGINT;
DECLARE VARIABLE ID BIGINT;
BEGIN
    INSERT INTO MONITOR_SC_ACAO(HORARIO, HOST_CLIENT,MONITOR_SC_ID)
        values ('now', :MG_HOST_CLIENTE, :MG_SC_ID) RETURNING ID into :acao_id;

     for
        SELECT
            CAST(p.PART as BIGINT)
        FROM
            PR_SPLIT_STRING (:IDS, ',') p
        INTO
            :ID
    do
    begin
        update PEDIDO p set p.MONITOR_SC_FALHA_ACAO_ID = :acao_id
            where p.ID = :ID and p.MONITOR_SC_FALHA_ACAO_ID is null;
    end
END^
SET TERM ; ^

UPDATE RDB$PROCEDURES set
  RDB$DESCRIPTION = 'Efetua a confirmação de que ocorreu uma falha no pedido no sc'
  where RDB$PROCEDURE_NAME = 'PR_CONFIRMA_FALHA_PEDIDO';








SET TERM ^ ;
CREATE TRIGGER LOJA_ENDERECO_BIU FOR LOJA_ENDERECO ACTIVE
BEFORE INSERT OR UPDATE POSITION 0
AS
BEGIN
    /**
    * Tratamento para garantir que só existirá apenas um endereço ativo por loja
    */

    if (new.STATUS_ENDERECO = 1 and (
        select count(0) from LOJA_ENDERECO le
        where
            le.LOJA_ID = new.LOJA_ID
            and le.STATUS_ENDERECO = 1
            and ENDERECO_ID <> new.ENDERECO_ID
        ) > 0) then
    begin
        /**
        * Como vamos ter um novo endereço como ativo tornamos o anterior desativado
        */
        update LOJA_ENDERECO set STATUS_ENDERECO = 0 where
            STATUS_ENDERECO = 1
            and LOJA_ID = new.LOJA_ID
            and ENDERECO_ID <> new.ENDERECO_ID;

    end

    update loja set LAST_UPDATE_INFOS = 'now' where id = new.LOJA_ID;

END^
SET TERM ; ^



SET TERM ^ ;

create procedure PR_REGISTRA_ARQUIVOS_USO_LOJA(loja_id int, arquivos_ids BLOB SUB_TYPE text)
AS
declare variable arquivos_ids_atuais BLOB SUB_TYPE text;
declare VARIABLE file_id bigint;
BEGIN

    SELECT
        list(r.ARQUIVO_DISPONIVEL_ID,';')
    FROM
        LOJA_ARQUIVO_DISPONIVEL r
    inner join ARQUIVO_DISPONIVEL ad
        on ad.ID = r.ARQUIVO_DISPONIVEL_ID
        and ad.EXISTE = 1
    where
        r.LOJA_ID = :loja_id and r.IS_EM_USO = 1
    order by
        1 asc
    into
        :arquivos_ids_atuais;

     if (:arquivos_ids_atuais <> :arquivos_ids or :arquivos_ids_atuais is null) then
    begin


        update LOJA_ARQUIVO_DISPONIVEL l set l.IS_EM_USO = null
            where l.LOJA_ID = :loja_id and l.IS_EM_USO is not null and l.TIPO < 1;

        for
            SELECT
                cast(p.PART as bigint)
            FROM
                PR_SPLIT_STRING (:arquivos_ids, ';') p
            into
                :file_id
        do
        begin

            update LOJA_ARQUIVO_DISPONIVEL l set l.IS_EM_USO = 1
                where l.LOJA_ID = :loja_id and l.ARQUIVO_DISPONIVEL_ID = :file_id and l.TIPO < 1;

        end


    end



END^

SET TERM ; ^
UPDATE RDB$RELATIONS set
  RDB$DESCRIPTION = 'Atualiza quais dos seus arquivos a loja de fato esta usando'
  where RDB$RELATION_NAME = 'PR_REGISTRA_ARQUIVOS_USO_LOJA';


SET TERM ^ ;
create PROCEDURE PR_REGISTRA_EXCLUSAO_ARQUIVO (
    ID_DEV BIGINT,
    ARQUIVOS_IDS BLOB SUB_TYPE 1,
    HOST_CLIENT DM_HOST_PORT )
AS
DECLARE VARIABLE file_id BIGINT;
    DECLARE VARIABLE ID BIGINT;
BEGIN

    INSERT INTO DISPOSITIVO_ACAO (DISPOSITIVO_ID, HOST_CLIENTE, HORARIO) VALUES (:ID_DEV, :HOST_CLIENT, 'NOW')
    RETURNING ID INTO :ID;


    for
        SELECT
            cast(p.PART as bigint)
        FROM
            PR_SPLIT_STRING (:arquivos_ids, ';') p
        into
            :file_id
    do
    begin
        update ARQUIVO_DOWNLOAD r
        set r.EXCLUSAO_DEV_ACAO_ID = :ID
        where
            r.EXCLUSAO_DEV_ACAO_ID is null
            and r.HORARIO_FIM_DOWNLOAD is not null
            and r.ARQUIVO_DISPONIVEL_ID = :file_id
            and r.DISPOSITIVO_ACAO_ID = (
                select da.ID
                from DISPOSITIVO_ACAO da
                where da.DISPOSITIVO_ID = :ID_DEV
                and r.DISPOSITIVO_ACAO_ID = da.ID
            );
    end

END^
SET TERM ; ^

UPDATE RDB$RELATIONS set
  RDB$DESCRIPTION = 'Registra que o dispositivo excluiu os arquivos do seu armazenamento local'
  where RDB$RELATION_NAME = 'PR_REGISTRA_EXCLUSAO_ARQUIVO';



CREATE VIEW VW_ARQUIVO_LOJA_PENDENTE_EXCLUS (ID, APP_PATH, DISPOSITIVO_ID, LOJA_ID)
AS
SELECT
    DISTINCT
    aad.ID,
    aad.APP_PATH,
    da.DISPOSITIVO_ID,
    r.LOJA_ID
FROM
    LOJA_ARQUIVO_DISPONIVEL r
inner join ARQUIVO_DOWNLOAD ad
    on ad.ARQUIVO_DISPONIVEL_ID = r.ARQUIVO_DISPONIVEL_ID
    and ad.EXCLUSAO_DEV_ACAO_ID is null
inner JOIN DISPOSITIVO_ACAO da
    on da.ID = ad.DISPOSITIVO_ACAO_ID
inner join ARQUIVO_DISPONIVEL aad
    on aad.ID = r.ARQUIVO_DISPONIVEL_ID
where
    (r.IS_EM_USO is null or r.IS_EM_USO = 0)
    and r.TIPO < 1
    and aad.APP_PATH not like '%json'
    and exists(
    --A loja precisa ter ao menos um arquivo em uso para passar a usar a validação
        select 1 from LOJA_ARQUIVO_DISPONIVEL
        where LOJA_ID = r.LOJA_ID and IS_EM_USO = 1
    )
    and not EXISTS(
    --O arquivo não pode existir em outra loja, pois é provável que o mesmo seja usado no processo de imagens comuns
        SELECT 1 FROM ARQUIVO_DISPONIVEL
        inner join LOJA_ARQUIVO_DISPONIVEL
            on ARQUIVO_DISPONIVEL_ID = ID
            and LOJA_ID <> r.LOJA_ID
        WHERE MD5 = aad.MD5 AND EXISTE = 1
    );
UPDATE RDB$RELATIONS set
  RDB$DESCRIPTION = 'Retorna os arquivos pendentes de exclusão para o dispositivo na loja'
  where RDB$RELATION_NAME = 'VW_ARQUIVO_LOJA_PENDENTE_EXCLUS';


SET TERM ^ ;
CREATE PROCEDURE PR_PREPARA_LISTA_LOJAS_NOVO (
    ID_DEV BIGINT )
RETURNS (
    ID DM_CNPJ,
    IS_DESTAQUE DM_BOOL,
    NOME DM_NOME,
    IMAGEM VARCHAR(150),
    ICONE VARCHAR(150),
    PATH_INFOS_FILE VARCHAR(250) )
AS
declare variable CNPJ DM_CNPJ;
declare variable CIDADE_ID INTEGER;
declare variable IMAGEM_FULL VARCHAR(150);
declare variable QTDE_PEDIDOS_FEITOS_NO_DEV INTEGER;
declare variable IS_TESTER SMALLINT;
BEGIN

    SELECT
        u.TIPO_USUARIO
    FROM
        USUARIO u
    JOIN
        VW_USUARIO_LOGADO ul
            ON u.ID = ul.USUARIO_ID
    WHERE
        ul.DISPOSITIVO_ID = :ID_DEV
    INTO :IS_TESTER;

    IF (:IS_TESTER IS NULL) THEN
    BEGIN
        IS_TESTER = 0;
    END


    FOR SELECT
            r.ID,
            r.CNPJ,
            r.NOME,
            r.CIDADE_ID,
            coalesce((select first 1 APP_PATH from VW_ARQUIVO_LOJA_DISPONIVEL where LOJA_ID = r.ID and TIPO = 1), 'not-found-img') as IMAGEM,
            coalesce((select first 1 APP_PATH from VW_ARQUIVO_LOJA_DISPONIVEL where LOJA_ID = r.ID and TIPO = 2), 'not-found-img') as IMAGEM_FULL,
            coalesce((select first 1 APP_PATH from VW_ARQUIVO_LOJA_DISPONIVEL where LOJA_ID = r.ID and TIPO = 3), 'not-found-img') as ICONE,
            coalesce(qpdl.QTDE, 0) as QTDE_PEDIDOS_FEITOS_NO_DEV,
            inf.PATH
        FROM
            VW_INFOS_LOJA r
        inner join LOJA l
            on l.ID = r.id
        inner join VW_ARQUIVO_LOJA_DISPONIVEL inf
            on inf.LOJA_ID = r.ID and inf.APP_PATH like '%infos-%'
        left join VW_QTDE_PEDIDO_DEV_LOJA qpdl
            on qpdl.LOJA_ID = r.ID and qpdl.DISPOSITIVO_ID = :ID_DEV

        where
            (r.LOJA_STATUS = 1
            or (
                r.LOJA_STATUS = 2
                AND :IS_TESTER = 1
            ))
            and l.IS_FUNCIONAMENTO_FX = 1
        ORDER BY
            QTDE_PEDIDOS_FEITOS_NO_DEV DESC,
            r.APP_INDICE_APRESENTACAO ASC
        INTO
            :ID, :CNPJ, :NOME, :CIDADE_ID, :IMAGEM, :IMAGEM_FULL,
            :ICONE, :QTDE_PEDIDOS_FEITOS_NO_DEV, :PATH_INFOS_FILE
    do
    begin
        IS_DESTAQUE = 0;

        if(:QTDE_PEDIDOS_FEITOS_NO_DEV > 0) then
        begin
            IS_DESTAQUE = 1;
            IMAGEM = IMAGEM_FULL;
        end
        else
        begin
            if ((
                select
                    count(0)
                FROM
                    CIDADE c
                inner join BAIRRO b
                    on b.CIDADE_ID = c.ID
                inner join ENDERECO e
                    on e.BAIRRO_ID = b.ID
                inner join LOJA_ENDERECO le
                    on le.ENDERECO_ID = e.ID
                inner join LOJA l
                    on l.ID = le.LOJA_ID
                where
                    c.CODIGO_IBGE is not null
                    AND b.CIDADE_ID = c.ID
                    and le.STATUS_ENDERECO = 1
                    and c.ID = :CIDADE_ID
                    and (l.LOJA_STATUS = 1 or (l.LOJA_STATUS = 2 AND :IS_TESTER = 1))
            ) <= 3) then
            begin
                IS_DESTAQUE = 1;
                IMAGEM = IMAGEM_FULL;
            end
        end

        ID = :CNPJ;

        SUSPEND;
    end


END^
SET TERM ; ^

UPDATE RDB$PROCEDURES set
  RDB$DESCRIPTION = 'Prepara e retorna a relação de LOJAS disponíveis para o dispositivo'
  where RDB$PROCEDURE_NAME = 'PR_PREPARA_LISTA_LOJAS_NOVO';


SET TERM ^ ;
create PROCEDURE PR_RETORNA_IMAGENS_FALTANTES (
    LISTA_MD5_IMGS BLOB SUB_TYPE 1 )
RETURNS (
    MD5 DM_MD5,
    APP_PATH VARCHAR(150) )
AS
BEGIN

    for
        SELECT
            DISTINCT
            p.PART,
            iif(ad.EXISTE = 0, null,
                (
                SELECT FIRST 1
                    ad.APP_PATH
                FROM
                    ARQUIVO_DISPONIVEL ad
                WHERE
                    ad.MD5 = p.PART
                    AND ad.EXISTE = 1
                ORDER BY
                    ad.ID ASC
                )
            )
        FROM PR_SPLIT_STRING (:LISTA_MD5_IMGS, ';') p
        left join ARQUIVO_DISPONIVEL ad
            on ad.MD5 = p.PART
        into :MD5, :APP_PATH
    do
    begin

        SUSPEND;

        MD5 = null;
        APP_PATH = null;

    end



END^
SET TERM ; ^

UPDATE RDB$PROCEDURES set
  RDB$DESCRIPTION = 'Recebe uma lista de md5 de imagens, separada por ";", e retorna quais desses md5 não estão no capp, bem como qual o app_path ideal para o mesmo'
  where RDB$PROCEDURE_NAME = 'PR_RETORNA_IMAGENS_FALTANTES';




CREATE GENERATOR GEN_ITEM_PEDIDO_SABOR_PROD_ID;
SET TERM ^ ;
CREATE TRIGGER PEDIDO_ITEM_SABOR_PROD2_BI FOR PEDIDO_ITEM_SABOR_PROD2 ACTIVE
BEFORE INSERT POSITION 0
AS
BEGIN
   NEW.ID = GEN_ID(GEN_ITEM_PEDIDO_SABOR_PROD_ID, 1);
END^
SET TERM ; ^


CREATE GENERATOR GEN_ARQUIVO_DISPONIVEL_ID;
SET TERM !! ;
CREATE TRIGGER ARQUIVO_DISPONIVEL_BI FOR ARQUIVO_DISPONIVEL
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_ARQUIVO_DISPONIVEL_ID, 1);
END!!
SET TERM ; !!


CREATE GENERATOR GEN_PEDIDO_OBSERVACAO_ID;
SET TERM ^ ;
CREATE TRIGGER PEDIDO_OBSERVACAO_BI FOR PEDIDO_OBSERVACAO ACTIVE
BEFORE INSERT POSITION 0
AS
BEGIN
   NEW.ID = GEN_ID(GEN_PEDIDO_OBSERVACAO_ID, 1);
END^
SET TERM ; ^


CREATE GENERATOR GEN_BAIRRO_ID;
SET TERM !! ;
CREATE TRIGGER BAIRRO_BI FOR BAIRRO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_BAIRRO_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_CATEGORIA_ID;
SET TERM !! ;
CREATE TRIGGER CATEGORIA_BI FOR CATEGORIA
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_CATEGORIA_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_CIDADE_ID;
SET TERM !! ;
CREATE TRIGGER CIDADE_BI FOR CIDADE
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_CIDADE_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_DISPOSITIVO_ID;
SET TERM !! ;
CREATE TRIGGER DISPOSITIVO_BI FOR DISPOSITIVO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_DISPOSITIVO_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_DISPOSITIVO_ACAO_ID;
SET TERM !! ;
CREATE TRIGGER DISPOSITIVO_ACAO_BI FOR DISPOSITIVO_ACAO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_DISPOSITIVO_ACAO_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_ENDERECO_ID;
SET TERM !! ;
CREATE TRIGGER ENDERECO_BI FOR ENDERECO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_ENDERECO_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_ESTADO_ID;
SET TERM !! ;
CREATE TRIGGER ESTADO_BI FOR ESTADO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_ESTADO_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_LOJA_ID;
SET TERM !! ;
CREATE TRIGGER LOJA_BI FOR LOJA
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_LOJA_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_LOJA_CREDITO_ID;
SET TERM !! ;
CREATE TRIGGER LOJA_CREDITO_BI FOR LOJA_CREDITO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_LOJA_CREDITO_ID, 1);
END!!
SET TERM ; !!

CREATE GENERATOR GEN_LOJA_BONUS_CONF_ID;
SET TERM !! ;
CREATE TRIGGER LOJA_BONUS_CONF_BI FOR LOJA_BONUS_CONF
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_LOJA_BONUS_CONF_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_MONITOR_SC_ID;
SET TERM !! ;
CREATE TRIGGER MONITOR_SC_BI FOR MONITOR_SC
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_MONITOR_SC_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_MONITOR_SC_ACAO_ID;
SET TERM !! ;
CREATE TRIGGER MONITOR_SC_ACAO_BI FOR MONITOR_SC_ACAO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_MONITOR_SC_ACAO_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_NOTIFICACAO_ID;
SET TERM !! ;
CREATE TRIGGER NOTIFICACAO_BI FOR NOTIFICACAO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_NOTIFICACAO_ID, 1);
END!!
SET TERM ; !!




CREATE GENERATOR GEN_PAIS_ID;
SET TERM !! ;
CREATE TRIGGER PAIS_BI FOR PAIS
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_PAIS_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_PEDIDO_ID;
SET TERM !! ;
CREATE TRIGGER PEDIDO_BI FOR PEDIDO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_PEDIDO_ID, 1);
END!!
SET TERM ; !!




CREATE GENERATOR GEN_SERVIDOR_ID;
SET TERM !! ;
CREATE TRIGGER SERVIDOR_BI FOR SERVIDOR
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_SERVIDOR_ID, 1);
END!!
SET TERM ; !!




CREATE GENERATOR GEN_TELEFONE_ID;
SET TERM !! ;
CREATE TRIGGER TELEFONE_BI FOR TELEFONE
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_TELEFONE_ID, 1);
END!!
SET TERM ; !!




CREATE GENERATOR GEN_USO_API_EXTERNA_ID;
SET TERM !! ;
CREATE TRIGGER USO_API_EXTERNA_BI FOR USO_API_EXTERNA
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_USO_API_EXTERNA_ID, 1);
END!!
SET TERM ; !!




CREATE GENERATOR GEN_USUARIO_ID;
SET TERM !! ;
CREATE TRIGGER USUARIO_BI FOR USUARIO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    if (NEW.SENHA IS NULL AND NEW.FACEBOOK_ID IS NULL) then exception EXCEP_USUARIO_INCOMPLETO;
    NEW.ID = GEN_ID(GEN_USUARIO_ID, 1);
END!!
SET TERM ; !!


CREATE GENERATOR GEN_USUARIO_TELEFONE_ID;
SET TERM !! ;
CREATE TRIGGER USUARIO_TELEFONE_BI FOR USUARIO_TELEFONE
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_USUARIO_TELEFONE_ID, 1);
END!!
SET TERM ; !!






SET TERM ^ ;
create PROCEDURE PR_REGISTRA_NOTI_P_5 (
    ID_USUARIO BIGINT,
    PEDIDO_ID BIGINT,
    MSG DM_TEXTO )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
declare VARIABLE loja_id int;
BEGIN


    select
        r.LOJA_ENDERECO_LOJA_ID
    FROM
        PEDIDO r
    where
        r.id = :PEDIDO_ID
    into
        :loja_id;

    SELECT
        ID
    FROM
        PR_REGISTRA_NOTI(
            :loja_id,
            'Pedido Confirmado',
            :msg,
            1,
            :ID_USUARIO,
            null
        )
    INTO :ID;

    UPDATE NOTIFICACAO SET
PEDIDO_ID = :PEDIDO_ID
WHERE ID = :ID and PEDIDO_ID IS NULL;

   SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;
    SUSPEND;
END^
SET TERM ; ^

UPDATE RDB$PROCEDURES set
  RDB$DESCRIPTION = 'Quando um pedido for confirmado'
  where RDB$PROCEDURE_NAME = 'PR_REGISTRA_NOTI_P_5';






create view VW_INFOS_PEDIDOS_CANCELAVEIS(
    ID, USUARIO, HORARIO, LOJA, TOTAL
) as
select
    p.ID,
    u.NOME_COMPLETO,
    da.HORARIO,
    l.NOME,
    p.TOTAL
from
    PEDIDO p
inner join LOJA l
    on l.ID = p.LOJA_ENDERECO_LOJA_ID
inner join USUARIO u
    on u.ID = p.USUARIO_ENDERECO_USUARIO_ID
inner join DISPOSITIVO_ACAO da
    on da.ID = p.DISPOSITIVO_ACAO_ID
where
    l.IS_FUNCIONAMENTO_FX = 1
    and p.IS_CANCELADO = 0
order by
    da.HORARIO desc;

comment on view VW_INFOS_PEDIDOS_CANCELAVEIS is 'Retorna os pedidos que podem ser cancelados pelo adm';



create view VW_INFOS_PEDIDOS_PENDENTES_CANC(
    ID, loja_id
) as
select
    p.ID,
    p.LOJA_ENDERECO_LOJA_ID
from
    PEDIDO p
inner join loja l
    on l.ID = p.LOJA_ENDERECO_LOJA_ID
inner join DISPOSITIVO_ACAO da
    on da.ID = p.DISPOSITIVO_ACAO_ID
where
    l.IS_FUNCIONAMENTO_FX = 1
    and p.IS_CANCELADO = 1
    and p.MONITOR_SC_CANCELADOR_ACAO_ID is null
    and p.MONITOR_SC_COLETOR_ACAO_ID is not null
order by
    da.HORARIO desc;

comment on view VW_INFOS_PEDIDOS_PENDENTES_CANC is 'Pedidos que foram cancelados por um adm e que estão pendentes de serem coletados pelo sc';

SET TERM ^ ;

CREATE PROCEDURE PR_CANCELA_PEDIDO_ADM
(
    id bigint
)
AS
BEGIN


    if ((select count(0) from PEDIDO p where p.ID = :ID) = 0) then
    BEGIN
        EXCEPTION EXCEP_PEDIDO_NOT_FOUND;
    end


    update PEDIDO p
        set p.IS_CANCELADO = 1
        where p.ID = :ID;


END^

SET TERM ; ^

comment on procedure PR_CANCELA_PEDIDO_ADM is 'Marca o pedido como cancelado, disparado por um adm';





SET TERM ^ ;
CREATE PROCEDURE PR_CONFIRMA_RECEBIMENTO_CANC (
    IDS VARCHAR(500),
    MG_HOST_CLIENTE DM_HOST_PORT,
    MG_SC_ID INTEGER )
AS
DECLARE VARIABLE acao_id BIGINT;
DECLARE VARIABLE ID BIGINT;
BEGIN
    INSERT INTO MONITOR_SC_ACAO(HORARIO, HOST_CLIENT,MONITOR_SC_ID)
        values ('now', :MG_HOST_CLIENTE, :MG_SC_ID) RETURNING ID into :acao_id;

     for
        SELECT
            CAST(p.PART as BIGINT)
        FROM
            PR_SPLIT_STRING (:IDS, ',') p
        INTO
            :ID
    do
    begin
        update PEDIDO p set p.MONITOR_SC_CANCELADOR_ACAO_ID = :acao_id
            where p.ID = :ID and p.MONITOR_SC_CANCELADOR_ACAO_ID is null;
    end
END^
SET TERM ; ^
comment on procedure PR_CONFIRMA_RECEBIMENTO_CANC is 'Efetua a confirmação do recebimento de um cancelamento de umpedido pelo MG';












SET TERM ^ ;

CREATE PROCEDURE PR_SYNC_LOJA
 (
    cnpj DM_CNPJ,
    nome DM_NOME,
    loja_status DM_LOJA_STATUS,
    app_indice_apresentacao int,
    logradouro DM_DESCRICAO,
    numero varchar(10),
    complemento DM_DESCRICAO_LONGA,
    cep DM_CEP,
    referencia DM_REFERENCIA,
    bairro DM_NOME,
    cidade DM_NOME,
    cidade_ibge char(7),
    estado DM_NOME,
    pais DM_NOME,
    percentual DM_PORCENTAGEM
 )
AS
DECLARE VARIABLE id int;
DECLARE VARIABLE id_endereco int;
BEGIN


    select r.ID from LOJA r where r.CNPJ = :CNPJ
        into :ID;

    if (:ID is null) then
        INSERT INTO LOJA (CNPJ, NOME, LOJA_STATUS, APP_INDICE_APRESENTACAO,JSON_TMP_INFOS,LAST_UPDATE_INFOS)
            VALUES (
                :CNPJ,
                :NOME,
                :loja_status,
                :app_indice_apresentacao,
                '{}',
                'now'
          ) RETURNING ID into :ID;
    else
        update loja set NOME = :nome, LOJA_STATUS = :loja_status, APP_INDICE_APRESENTACAO = :app_indice_apresentacao
            where ID = :ID;




    SELECT p.ID FROM PR_REGISTRA_ENDERECO (:LOGRADOURO, :NUMERO, :COMPLEMENTO, :REFERENCIA, :CEP, null, null,
         :BAIRRO, null, :CIDADE, null, :ESTADO, null, :PAIS, null) p
         into :id_endereco;

    if ( (select count(0) from CIDADE r where r.CODIGO_IBGE = :cidade_ibge) = 0) then
        update CIDADE c set c.CODIGO_IBGE = :cidade_ibge where c.NOME = :CIDADE and c.CODIGO_IBGE is null;


    if ( (select count(0) from LOJA_ENDERECO r where r.LOJA_ID = :id and r.ENDERECO_ID = :id_endereco) = 0) then
        INSERT INTO LOJA_ENDERECO (ENDERECO_ID, STATUS_ENDERECO, LOJA_ID)VALUES (:id_endereco, 1, :ID);

    if ( (select count(0) from LOJA_BONUS_CONF r where r.LOJA_ID = :id and r.PERCENTUAL = :percentual) = 0) then
        INSERT INTO LOJA_BONUS_CONF (LOJA_ID, PERCENTUAL, STATUS_BONUS)VALUES (:ID,:percentual,1);


END^

SET TERM ; ^

comment on procedure PR_SYNC_LOJA is 'Efetua os cadastros internos para a loja importada';
SET TERM ^ ;

CREATE PROCEDURE PR_SYNC_DISPOSITIVO
 (
    id_hardware varchar(100),
    app_versao int,
    app_token_fcm varchar(300),
    so DM_SO_DISPOSITIVO,
    usuario_logado_cpf dm_cpf,
    usuario_logado_nome DM_NOME,
    usuario_logado_sobrenome DM_SOBRENOME,
    usuario_logado_email DM_EMAIL,
    usuario_logado_facebook_id varchar(35),
    usuario_logado_senha varchar(100),
    usuario_logado_numeros_tel blob sub_type 1,
    usuario_logado_enderecos_ids blob sub_type 1
 )
AS
DECLARE VARIABLE id_dev bigint;
DECLARE VARIABLE id_usr bigint = null;
DECLARE VARIABLE tel_num DM_TELEFONE;
DECLARE VARIABLE aux_id bigint;
BEGIN

    select id from PR_RETORNA_CADASTRO_DISPOSITIVO(:id_hardware) into :id_dev;

    if(:usuario_logado_cpf is not null) then
    begin
        select
            ID
        from
            USUARIO
        where
            cpf = :usuario_logado_cpf
        into
            :id_usr;

        if(:id_usr is null) then
        begin
            INSERT INTO DISPOSITIVO_ACAO (HORARIO, HOST_CLIENTE, DISPOSITIVO_ID)
                values ('now', 'sync', :id_dev) RETURNING ID INTO :aux_id;

            INSERT INTO USUARIO (CPF, DISPOSITIVO_ACAO_CADASTRO_ID, EMAIL, NOME, SOBRENOME,
            SENHA, TIPO_USUARIO, FACEBOOK_ID) values (:usuario_logado_cpf, :aux_id, :usuario_logado_email, :usuario_logado_nome, :usuario_logado_sobrenome, :usuario_logado_senha, 0, :usuario_logado_facebook_id)
            RETURNING ID INTO :id_usr;

            for
                select
                    s.PART
                from
                    PR_SPLIT_STRING(:usuario_logado_numeros_tel, ',') s
                left join TELEFONE t
                    on t.NUMERO = s.PART
                where
                    t.ID is null
                into
                    :tel_num
            do
            begin
                INSERT INTO TELEFONE(NUMERO) VALUES (:tel_num) returning ID INTO :aux_id;
                INSERT INTO USUARIO_TELEFONE (USUARIO_ID, TELEFONE_ID) VALUES (:id_usr,:aux_id);
            end

            for
                select
                    s.PART
                from
                    PR_SPLIT_STRING(:usuario_logado_enderecos_ids, ',') s
                left join USUARIO_ENDERECO e
                    on e.ENDERECO_ID = s.PART and e.USUARIO_ID = :id_usr
                where
                    e.ENDERECO_ID is null
                into
                    :aux_id
            do
            begin
                INSERT INTO USUARIO_ENDERECO(endereco_id, usuario_id) values (:aux_id, :id_usr);

            end

        end

        execute procedure PR_REGISTRA_LOGADO(:id_usr, :id_dev, 'sync', 0);

        execute procedure PR_REGISTRA_LOGADO(:id_usr, :id_dev, 'sync', 1);


    end

END^

SET TERM ; ^


comment on procedure PR_SYNC_DISPOSITIVO is 'Efetua os cadastros internos para um dispositivo importado';





SET TERM ^ ;

CREATE PROCEDURE PR_MIGRA_DISPOSITIVO_SERVER
 (
    id_dev bigint,
    id_capp_origem int,
    id_capp_destino int
 )
AS
BEGIN

    delete from DISPOSITIVO_SERVIDOR_DEFAULT dsd
        where dsd.DISPOSITIVO_ID = :id_dev;

    insert into DISPOSITIVO_SERVIDOR_DEFAULT(DISPOSITIVO_ID, SERVIDOR_ID, INDICE)
        values (:id_dev, :id_capp_destino, 1);




END^

SET TERM ; ^

comment on procedure PR_MIGRA_DISPOSITIVO_SERVER is 'Efetua a migração do dispositivo entre os capps';
SET TERM ^ ;

CREATE PROCEDURE PR_MIGRA_LOJA_SERVER
 (
    id_loja int,
    id_capp_origem int,
    id_capp_destino int
 )
AS
BEGIN

    delete from LOJA_SERVIDOR_DEFAULT lsd
        where lsd.LOJA_ID = :id_loja;

    insert into LOJA_SERVIDOR_DEFAULT(LOJA_ID, SERVIDOR_ID, INDICE)
        values (:id_loja, :id_capp_destino, 1);




END^

SET TERM ; ^

comment on procedure PR_MIGRA_LOJA_SERVER is 'Efetua a migração da loja entre os capps';


create view vw_host_direcionado_dispositivo (
    DISPOSITIVO_ID,
    HOST
) as select
    dsd.DISPOSITIVO_ID,
    s.HOST
from
    DISPOSITIVO_SERVIDOR_DEFAULT dsd
inner join SERVIDOR s
    on s.ID = dsd.SERVIDOR_ID;

comment on view vw_host_direcionado_dispositivo is 'Retorna o host que o dispositivo deve usar';


create view vw_host_direcionado_loja(
    LOJA_ID, HOST
) as select
    lsd.LOJA_ID,
    s.HOST
from
    LOJA_SERVIDOR_DEFAULT lsd
inner join SERVIDOR s
    on s.ID = lsd.SERVIDOR_ID;


comment on view vw_host_direcionado_loja is 'Retorna o host que o dispositivo deve usar';




SET TERM ^ ;
CREATE PROCEDURE PR_MIGRA_PEDIDOS_SWING_TO_WEB (
    LOJA_ID_SWING INTEGER,
    LOJA_ID_WEB INTEGER )
AS

DECLARE VARIABLE PEDIDO_ID INTEGER;
DECLARE VARIABLE LOJA_WEB_ENDERECO_ID INTEGER;
DECLARE VARIABLE LOJA_WEB_BONUS_ID INTEGER;

BEGIN

    SELECT e.ENDERECO_ID FROM LOJA_ENDERECO e WHERE e.LOJA_ID = :LOJA_ID_WEB AND e.STATUS_ENDERECO <> 0 INTO LOJA_WEB_ENDERECO_ID;
    SELECT FIRST 1 a.ID FROM LOJA_BONUS_CONF a WHERE a.LOJA_ID = :LOJA_ID_WEB AND a.STATUS_BONUS = 1 INTO LOJA_WEB_BONUS_ID;

    FOR SELECT p.ID FROM PEDIDO p WHERE p.LOJA_ENDERECO_LOJA_ID = :LOJA_ID_SWING
    INTO :PEDIDO_ID
    DO
        UPDATE PEDIDO pe
        SET
            pe.LOJA_ENDERECO_ENDERECO_ID = :LOJA_WEB_ENDERECO_ID,
            pe.LOJA_ENDERECO_LOJA_ID = :LOJA_ID_WEB,
            pe.LOJA_BONUS_CONF_ID = :LOJA_WEB_BONUS_ID
        WHERE
            pe.ID = :PEDIDO_ID;

END^
SET TERM ; ^






SET TERM ^ ;
CREATE PROCEDURE PR_CANCELA_PEDIDO (
    ID BIGINT,
    MG_HOST_CLIENTE DM_HOST_PORT,
    MG_SC_ID INTEGER )
AS
declare variable acao_id bigint;
BEGIN

    /**
    * Cancelamento do pedido
    */


    if ((select count(0) from PEDIDO p where p.ID = :ID) = 0) then
    BEGIN
        EXCEPTION EXCEP_PEDIDO_NOT_FOUND;
    end


    if ((select count(0) from PEDIDO p where p.ID = :ID and p.MONITOR_SC_CANCELADOR_ACAO_ID is not null) > 0) then
    BEGIN
        EXCEPTION EXCEP_CONFIRMACAO_PED_DUPLICADA;
    end

    INSERT INTO MONITOR_SC_ACAO(HORARIO, HOST_CLIENT,MONITOR_SC_ID)
        values ('now',:MG_HOST_CLIENTE, :MG_SC_ID) RETURNING ID into :acao_id;

    update PEDIDO p set MONITOR_SC_CANCELADOR_ACAO_ID = :acao_id, IS_CANCELADO = 1
        where p.ID = :ID;
    --Bugfix: caso o mg não tenha enviado ao capp o aviso de que recebeu o pedido
    update PEDIDO p set p.MONITOR_SC_COLETOR_ACAO_ID = :acao_id
        where p.ID = :ID AND p.MONITOR_SC_COLETOR_ACAO_ID is null;

END^
SET TERM ; ^
comment on procedure PR_CANCELA_PEDIDO is 'Efetua o cancelamento de um pedido';







SET TERM ^ ;
create PROCEDURE PR_REGISTRA_NOTI_P_6 (
    ID_USUARIO BIGINT,
    PEDIDO_ID BIGINT,
    MSG DM_TEXTO )
RETURNS (
    ID BIGINT,
    CONTEUDO dm_texto,
    TITULO VARCHAR(150),
    TIPO DM_NOTIFICACAO_TIPO,
    RESP CHAR(14),
    TOKEN VARCHAR(300) )
AS
declare VARIABLE loja_id int;
BEGIN


    select
        r.LOJA_ENDERECO_LOJA_ID
    FROM
        PEDIDO r
    where
        r.id = :PEDIDO_ID
    into
        :loja_id;

    SELECT
        ID
    FROM
        PR_REGISTRA_NOTI(
            :loja_id,
            'Pedido cancelado',
            :msg,
            1,
            :ID_USUARIO,
            null
        )
    INTO :ID;

   SELECT
            n.ID,
            n.CONTEUDO,
            n.TITULO,
            n.TIPO,
            (select l.CNPJ FROM LOJA l WHERE l.ID = n.LOJA_RESPONSAVEL_ID),
            (SELECT TOKEN FROM PR_REGISTRA_REQUISICAO_FCM(coalesce(
                um.DISPOSITIVO_ID,
                (select r.DISPOSITIVO_ID from VW_USUARIO_ULTIMO_DISPO_LOGADO r where r.USUARIO_ID = nu.USUARIO_ID)
            ))) as TOKEN
        FROM
            NOTIFICACAO_USUARIO nu
        JOIN
            NOTIFICACAO n ON n.ID = nu.NOTIFICACAO_ID
       left JOIN
           VW_USUARIO_LOGADO um ON um.USUARIO_ID = nu.USUARIO_ID
        WHERE
            nu.D_A_RECEBIMENTO_ID is NULL
            AND n.ID = :ID
        INTO
            :ID, :CONTEUDO, :TITULO, :TIPO, :RESP, :TOKEN;
    SUSPEND;
END^
SET TERM ; ^

UPDATE RDB$PROCEDURES set
  RDB$DESCRIPTION = 'Quando um pedido for cancelado'
  where RDB$PROCEDURE_NAME = 'PR_REGISTRA_NOTI_P_6';







SET TERM ^ ;
CREATE PROCEDURE PR_VALIDA_LOGIN_MONITORAMENTO (
    EMAIL DM_EMAIL,
    SENHA VARCHAR(100) )
RETURNS (
    OK DM_BOOL
)
AS
BEGIN

    /**
    * Valida se o usuário e senha existem e se o mesmo pode acessar o monitoramento web
    *
    */

    if (position('@' in :email) = 0) then
    begin
        --É pra ser um cpf
        if (char_length(:email) > 11) then
        begin
            OK = 0;
            SUSPEND;
            exit;
        end
    end


    select
        count(0)
    from
        USUARIO u
    where
        u.SENHA = :senha
        AND (
                u.EMAIL = :EMAIL
                OR u.CPF = SUBSTRING(:EMAIL from 1 for 11)
            )
        and exists(
            select 1
            from VW_USUARIO_MONITORAMENTO
            where USUARIO_ID = u.ID
        )
    into
        :OK;

    SUSPEND;

END^
SET TERM  ; ^
comment on procedure PR_VALIDA_LOGIN_MONITORAMENTO is 'Efetua a validação do login para um usuário monitor(usado pelo egula web)';







create view VWR_PEDIDOS_MONITORAMENTO (ID, USUARIO, LOJA, TOTAL, EMISSAO,
    RECEBIMENTO_SC, CONFIRMACAO_SC, RECEBIMENTO_NOTIFICACAO_USUARIO, distancia)
as

with notis as (
    select
        max(da.HORARIO) as HORARIO,
        n.PEDIDO_ID
    from
        NOTIFICACAO n
    inner join NOTIFICACAO_USUARIO nu
        on nu.NOTIFICACAO_ID = n.ID
    inner join DISPOSITIVO_ACAO da
        on da.ID = nu.D_A_RECEBIMENTO_ID
    where
        n.TIPO <= 1
    group by
        2
)

select

    p.ID,
    u.NOME_COMPLETO,
    l.NOME,
    p.TOTAL,
    da.HORARIO,
    msa.HORARIO,
    msa2.HORARIO,
    n.HORARIO,
    p.DISTANCIA_LOJA_ENTREGA
from
    PEDIDO p
inner join DISPOSITIVO_ACAO da
    on da.ID = p.DISPOSITIVO_ACAO_ID
inner join USUARIO u
    on u.ID = p.USUARIO_ENDERECO_USUARIO_ID
inner join loja l
    on l.ID = p.LOJA_ENDERECO_LOJA_ID
left join MONITOR_SC_ACAO msa
    on msa.ID = p.MONITOR_SC_COLETOR_ACAO_ID
left join MONITOR_SC_ACAO msa2
    on msa2.ID = p.MONITOR_SC_CONFIRMADOR_ACAO_ID
left join notis n
    on n.PEDIDO_ID = p.ID
where p.IS_CANCELADO = 0
order by
    p.ID desc;

comment on view VWR_PEDIDOS_MONITORAMENTO is 'Relatório de pedidos usado pelo monitoramento';


CREATE VIEW VWR_GERENCIAL_EGULA (HORARIO, LOJA, TOTAL, USUARIO, CONFIRMACAO,
    SCID)
as
select
    da.HORARIO,
    l.NOME,
    p.TOTAL,
    u.NOME_COMPLETO,
    sc.HORARIO,
    sc.ID
from
    PEDIDO p
inner join LOJA l
    on l.ID = p.LOJA_ENDERECO_LOJA_ID
inner join USUARIO u
    on u.ID = p.USUARIO_ENDERECO_USUARIO_ID
inner join DISPOSITIVO_ACAO da
    on da.ID = p.DISPOSITIVO_ACAO_ID
left join MONITOR_SC_ACAO sc
    on sc.ID = p.MONITOR_SC_CONFIRMADOR_ACAO_ID
where
    p.IS_CANCELADO = 0
order by
    da.HORARIO desc;




create view VW_HOST_DIRECIONADO_LOJA_DEV (DISPOSITIVO_ID, CNPJ, HOST)
as
select
    r.DISPOSITIVO_ID,
    l.CNPJ,
    s.HOST
from
    DISPOSITIVO_LOJA_SERVIDOR r
inner join loja l
    on l.id = r.LOJA_ID
inner join SERVIDOR s
    on s.ID = r.SERVIDOR_ID;

comment on view VW_HOST_DIRECIONADO_LOJA_DEV is 'Retorna a lista de hosts que o dispositivo deve usar para comunicar com cada loja';

CREATE VIEW VWR_USUARIOS_NOVO (NOME, CODIGO, CPF, BONUS, CADASTRO, EMAIL, TELEFONE,
    CIDADE)
AS
SELECT
    u.NOME_COMPLETO AS NOME,
    u.ID AS CODIGO,
    u.CPF,
    coalesce(blu.BONUS,0)  as BONUS,
    dac.HORARIO AS CADASTRO,
    u.EMAIL,
    /*TELEFONE*/
    (SELECT first 1
         r.NUMERO
     FROM
         VW_INFOS_USUARIO_TELEFONE r
     where
         r.USUARIO_ID = u.ID
     order by
         r.TELEFONE_ID desc) AS TELEFONE,
    /*CIDADE*/
    coalesce((SELECT first 1
         r.CIDADE
     FROM
         VW_INFOS_USUARIO_ENDERECO r
     where
         r.USUARIO_ID = u.id
     order by
         r.ID desc), 'Não encontrada') as CIDADE


FROM
    USUARIO u
inner join DISPOSITIVO_ACAO dac
    ON dac.ID = u.DISPOSITIVO_ACAO_CADASTRO_ID
left join vw_bonus_livre_usuario blu
    on blu.usuario_id = u.id;

comment on view VWR_USUARIOS_NOVO is 'Relatório de usuário cadastros';





CREATE GENERATOR GEN_horario_atendimento_ID;

SET TERM !! ;
CREATE TRIGGER horario_atendimento_BI FOR horario_atendimento
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_horario_atendimento_ID, 1);
END!!
SET TERM ; !!

CREATE GENERATOR GEN_FORMA_PAGAMENTO_ID;

SET TERM !! ;
CREATE TRIGGER FORMA_PAGAMENTO_BI FOR FORMA_PAGAMENTO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_FORMA_PAGAMENTO_ID, 1);
END!!
SET TERM ; !!

CREATE GENERATOR GEN_GRUPO_PRODUTO_ID;

SET TERM !! ;
CREATE TRIGGER GRUPO_PRODUTO_BI FOR GRUPO_PRODUTO
ACTIVE BEFORE INSERT POSITION 0
AS

BEGIN

    NEW.ID = GEN_ID(GEN_GRUPO_PRODUTO_ID, 1);

END!!
SET TERM ; !!


CREATE GENERATOR GEN_PRODUTO_ID;

SET TERM !! ;
CREATE TRIGGER PRODUTO_BI FOR PRODUTO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN

    NEW.ID = GEN_ID(GEN_PRODUTO_ID, 1);

END!!
SET TERM ; !!


CREATE GENERATOR GEN_SABOR_PRODUTO_ID;

SET TERM !! ;
CREATE TRIGGER SABOR_PRODUTO_BI FOR sabor
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
   NEW.ID = GEN_ID(GEN_SABOR_PRODUTO_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_TAMANHO_PRODUTO_ID;

SET TERM !! ;
CREATE TRIGGER TAMANHO_PRODUTO_BI FOR TAMANHO_PRODUTO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN

    NEW.ID = GEN_ID(GEN_TAMANHO_PRODUTO_ID, 1);

END!!
SET TERM ; !!




CREATE GENERATOR GEN_OPCIONAL_PRODUTO_ID;

SET TERM !! ;
CREATE TRIGGER OPCIONAL_PRODUTO_BI FOR OPCIONAL_PRODUTO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
   NEW.ID = GEN_ID(GEN_OPCIONAL_PRODUTO_ID, 1);
END!!
SET TERM ; !!





CREATE GENERATOR GEN_FORMA_ENTREGA_ID;

SET TERM ^;

CREATE TRIGGER FORMA_ENTREGA_BI FOR FORMA_ENTREGA
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_FORMA_ENTREGA_ID, 1);
END^

SET TERM ;^



CREATE GENERATOR GEN_PEDIDO_ITEM_ID;

SET TERM ^;

CREATE TRIGGER PEDIDO_ITEM_BI FOR PEDIDO_ITEM
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_PEDIDO_ITEM_ID, 1);
END^

SET TERM ;^

set term ^;

create trigger FORMA_ENTREGA_BIU for FORMA_ENTREGA
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^
set term ^;

create trigger FORMA_PAGAMENTO_BIU for FORMA_PAGAMENTO
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger GRUPO_PRODUTO_BIU for GRUPO_PRODUTO
active before insert or update position 1
as
begin

    if (new.CONTROLE_MODULO is null or new.GRUPO_PRODUTO_ID IS DISTINCT FROM old.GRUPO_PRODUTO_ID) then
    begin
        select
            coalesce(max(p.CONTROLE_MODULO), 0) + 1
        from
            GRUPO_PRODUTO p
        where
            p.loja_id = new.LOJA_ID
            and p.ID <> new.ID
            and (
                (p.GRUPO_PRODUTO_ID is null and new.GRUPO_PRODUTO_ID is null)
                or
                (p.GRUPO_PRODUTO_ID = new.GRUPO_PRODUTO_ID and new.GRUPO_PRODUTO_ID is not null)
            )
        into
            new.CONTROLE_MODULO;
    end

    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger PRODUTO_BIU for PRODUTO
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger SABOR_BIU for SABOR
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger TAMANHO_PRODUTO_BIU for TAMANHO_PRODUTO
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger OPCIONAL_PRODUTO_BIU for OPCIONAL_PRODUTO
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^


set term ^;

create trigger SABOR_PRODUTO_DISPONIVEL_BIU for SABOR_PRODUTO_DISPONIVEL
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger SABOR_PRODUTO_INGREDIENTE_BIU for SABOR_PRODUTO_INGREDIENTE
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger TAMANHO_PRODUTO_DISPONIVEL_BIU for TAMANHO_PRODUTO_DISPONIVEL
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger PRODUTO_INGREDIENTE_BIU for PRODUTO_INGREDIENTE
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger OP_PROD_VALOR_TAMANHO_PROD_BIU for OP_PROD_VALOR_TAMANHO_PROD
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^

set term ^;

create trigger OPCIONAL_PRODUTO_DISPONIVEL_BIU for OPCIONAL_PRODUTO_DISPONIVEL
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';
end^

set term ;^



set term ^;

create trigger horario_atendimento_BIU for horario_atendimento
active before insert or update position 1
as
begin
    new.LAST_UPDATE = 'now';

    if (new.DATA_EXATA is not null) then
        new.DIA_SEMANA = null;

    if (new.DATA_EXATA is null and new.DIA_SEMANA is null) then
        new.DIA_SEMANA = 1;
end^

set term ;^



SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_PEDIDO_NOVO (
    DESTINO_LOGRADOURO DM_DESCRICAO,
    DESTINO_NUMERO varchar(10),
    DESTINO_COMPLEMENTO DM_DESCRICAO,
    DESTINO_REFERENCIA DM_REFERENCIA,
    DESTINO_CEP DM_CEP,
    DESTINO_BAIRRO DM_NOME,
    DESTINO_CIDADE DM_NOME,
    DESTINO_ESTADO DM_NOME,
    DESTINO_PAIS DM_NOME,
    DESTINO_ENDERECO_ID_ALT integer,
    LOJA_ID integer,
    AUTOR_DISPOSITIVO_ID bigint,
    AUTOR_DISPOSITIVO_HOST_CLIENTE DM_HOST_PORT,
    AUTOR_TELEFONE DM_TELEFONE,
    PEDIDO_PAGO_EM_BONUS DM_BOOL,
    FORMA_ENTREGA_ID integer,
    FORMA_PAGAMENTO_ID integer,
    OBS_USUARIO DM_TEXTO,
    VALOR_TROCO DM_MONEY )
RETURNS (
    ID_PEDIDO bigint )
AS
declare VARIABLE loja_endereco_id integer;
declare VARIABLE loja_bonus_id integer;
declare VARIABLE dev_acao_id bigint;
declare VARIABLE tel_user_id bigint;
declare VARIABLE tel_id bigint;
declare VARIABLE AUTOR_USUARIO_ID bigint;
BEGIN
    /**
    * Registra um novo pedido na base para ser posteriormente disponibilizados
    * aos SC''s
    */
    SELECT
        r.ENDERECO_ID,
        r.PERCENTUAL_ID
    FROM
        VW_INFOS_LOJA r
    where
        r.ID = :LOJA_ID
    into
        :loja_endereco_id, :loja_bonus_id;
    AUTOR_USUARIO_ID = null;
    select
        r.USUARIO_ID
    FROM
        VW_USUARIO_LOGADO r
    where
        r.DISPOSITIVO_ID = :AUTOR_DISPOSITIVO_ID
    into
        :AUTOR_USUARIO_ID;
    if (:AUTOR_USUARIO_ID is null) then
    begin
        SELECT
            r.ID
        FROM
            USUARIO r
        where
            r.CPF = '11111111111'
        into
            :AUTOR_USUARIO_ID;
    end
    insert into DISPOSITIVO_ACAO (DISPOSITIVO_ID, HOST_CLIENTE,HORARIO)
        values (:AUTOR_DISPOSITIVO_ID,:AUTOR_DISPOSITIVO_HOST_CLIENTE, 'now')
        RETURNING ID into :dev_acao_id;
    if (:DESTINO_ENDERECO_ID_ALT is null) then
    begin
        if (:DESTINO_LOGRADOURO is not null) then
        begin
            IN AUTONOMOUS TRANSACTION DO
            BEGIN
                SELECT
                    p.ID
                FROM
                    PR_REGISTRA_ENDERECO (:DESTINO_LOGRADOURO, :DESTINO_NUMERO,
                        :DESTINO_COMPLEMENTO, :DESTINO_REFERENCIA, :DESTINO_CEP, null,null, :DESTINO_BAIRRO, null,
                        :DESTINO_CIDADE, null, :DESTINO_ESTADO,
                        null, :DESTINO_PAIS, null) p
                into :DESTINO_ENDERECO_ID_ALT;
                 if ((select count(0) from USUARIO_ENDERECO r
                        where r.ENDERECO_ID = :DESTINO_ENDERECO_ID_ALT
                            and r.USUARIO_ID = :AUTOR_USUARIO_ID) = 0) then
                begin
                    insert into USUARIO_ENDERECO(ENDERECO_ID, USUARIO_ID)
                        values (:DESTINO_ENDERECO_ID_ALT,:AUTOR_USUARIO_ID);
                end
            end
        end
        else
        begin
            /**
            * Casos onde o consumo é local. Para esses casos relacionamos o endereço
            * do restaurante com o usuário. Esses enderços não são retornados pela
            * VW_INFOS_USUARIO_ENDERECO
            */
            if ((select count(0) from USUARIO_ENDERECO r
                where r.ENDERECO_ID = :loja_endereco_id
                    and r.USUARIO_ID = :AUTOR_USUARIO_ID) = 0) then
            begin
                insert into USUARIO_ENDERECO(ENDERECO_ID, USUARIO_ID)
                    values (:loja_endereco_id,:AUTOR_USUARIO_ID);
            end
            DESTINO_ENDERECO_ID_ALT = :loja_endereco_id;
        end
    end
    SELECT FIRST 1
        r.ID
    FROM
        VW_INFOS_USUARIO_TELEFONE r
    where
        r.USUARIO_ID = :AUTOR_USUARIO_ID
        and r.NUMERO = :AUTOR_TELEFONE
    into :tel_user_id;
    /**
    * Se o telefone que o usuário passou ainda não pertencer a ele vamos 
    * cadastrar
    */
    if (:tel_user_id is null) then
    begin
        INSERT INTO TELEFONE (NUMERO) VALUES (:AUTOR_TELEFONE) returning ID into :tel_id;
        INSERT INTO USUARIO_TELEFONE (USUARIO_ID, TELEFONE_ID) VALUES (:AUTOR_USUARIO_ID,:tel_id)
        returning ID into :tel_user_id;
    end
    INSERT INTO PEDIDO (
        LOJA_BONUS_CONF_ID,
        LOJA_ENDERECO_ENDERECO_ID,
        LOJA_ENDERECO_LOJA_ID,
        USUARIO_ENDERECO_USUARIO_ID,
        USUARIO_ENDERECO_ENDERECO_ID,
        DISPOSITIVO_ACAO_ID,
        PAGO_EM_BONUS,
        USUARIO_TELEFONE_ID,
        valor_entrega_cobrado,
        valor_pagamento_cobrado,
        valor_acrescimo_cobrado,
        valor_troco,
        forma_entrega_id,
        forma_pagamento_id
        )
    VALUES (
        :loja_bonus_id,
        :loja_endereco_id,
        :LOJA_ID,
        :AUTOR_USUARIO_ID,
        :DESTINO_ENDERECO_ID_ALT,
        :dev_acao_id,
        :PEDIDO_PAGO_EM_BONUS,
        :tel_user_id,
        null,
        null,
        null,
        :valor_troco,
        (
            select ID 
            from FORMA_ENTREGA 
            where 
                LOJA_ID = :LOJA_ID 
                and (BASE_CONTRATANTE_ID = :forma_entrega_id or ID = :forma_entrega_id)
        ),
        (
            select ID 
            from FORMA_PAGAMENTO 
            where 
                LOJA_ID = :LOJA_ID 
                and (BASE_CONTRATANTE_ID = :forma_pagamento_id or ID = :forma_pagamento_id)
        )
    )
    RETURNING ID INTO :ID_PEDIDO;
  SUSPEND;
END^
SET TERM ; ^
UPDATE RDB$PROCEDURES set
  RDB$DESCRIPTION = 'Efetua o registro de um pedido'
  where RDB$PROCEDURE_NAME = 'PR_REGISTRA_PEDIDO_NOVO';
GRANT EXECUTE
 ON PROCEDURE PR_REGISTRA_PEDIDO_NOVO TO  SYSDBA;


SET TERM ^ ;
CREATE PROCEDURE PR_REGISTRA_PEDIDO_REST (
    LOJA_ID integer,
    AUTOR_USUARIO_ID integer,
    AUTOR_DISPOSITIVO_ID bigint,
    AUTOR_DISPOSITIVO_HOST_CLIENTE DM_HOST_PORT,
    FORMA_PAGAMENTO_ID integer,
    PEDIDO_PAGO_EM_BONUS DM_BOOL,
    VALOR_TROCO DM_MONEY,
    VALOR_ENTREGA DM_MONEY,
    FORMA_ENTREGA_ID integer,
    DESTINO_ENDERECO_ID_ALT integer,
    OBS_USUARIO DM_TEXTO )
RETURNS (
    ID_PEDIDO bigint )
AS
declare VARIABLE loja_endereco_id integer;
declare VARIABLE loja_bonus_id integer;
declare VARIABLE dev_acao_id bigint;
declare VARIABLE tel_user_id bigint;
BEGIN
    /*
    * Registra um novo pedido na base para ser posteriormente disponibilizados
    * aos SC''s
    */
    SELECT
        r.ENDERECO_ID,
        r.PERCENTUAL_ID
    FROM
        VW_INFOS_LOJA r
    where
        r.ID = :LOJA_ID
    into
        :loja_endereco_id, :loja_bonus_id;
    -- Por segurana, se não vier o ID do usuário, gravamos mesmo assim com dados fixos.
    if (:AUTOR_USUARIO_ID is null) then
    begin
        SELECT
            r.ID
        FROM
            USUARIO r
        where
            r.CPF = '11111111111'
        into
            :AUTOR_USUARIO_ID;
    end
    insert into DISPOSITIVO_ACAO (DISPOSITIVO_ID, HOST_CLIENTE,HORARIO)
        values (:AUTOR_DISPOSITIVO_ID,:AUTOR_DISPOSITIVO_HOST_CLIENTE, 'now')
        RETURNING ID into :dev_acao_id;
    if (:DESTINO_ENDERECO_ID_ALT IS NOT NULL) then
        begin
            if ((select count(0) from USUARIO_ENDERECO r where r.ENDERECO_ID = :DESTINO_ENDERECO_ID_ALT and r.USUARIO_ID = :AUTOR_USUARIO_ID) = 0) then
                begin
                    insert into USUARIO_ENDERECO(ENDERECO_ID, USUARIO_ID)
                        values (:DESTINO_ENDERECO_ID_ALT,:AUTOR_USUARIO_ID);
                end
            end
        else
            begin
            /*
            * Casos onde o consumo é local. Para esses casos relacionamos o endereço
            * do restaurante com o usuário. Esses enderços não são retornados pela
            * VW_INFOS_USUARIO_ENDERECO
            */
            if ((select count(0) from USUARIO_ENDERECO r
                where r.ENDERECO_ID = :loja_endereco_id
                    and r.USUARIO_ID = :AUTOR_USUARIO_ID) = 0) then
            begin
                insert into USUARIO_ENDERECO(ENDERECO_ID, USUARIO_ID)
                    values (:loja_endereco_id,:AUTOR_USUARIO_ID);
            end
            DESTINO_ENDERECO_ID_ALT = :loja_endereco_id;
        end
    SELECT FIRST 1
        r.ID 
    FROM
        USUARIO_TELEFONE r
    where
        r.USUARIO_ID = :AUTOR_USUARIO_ID
    into :tel_user_id;
    INSERT INTO PEDIDO (
        LOJA_BONUS_CONF_ID,
        LOJA_ENDERECO_ENDERECO_ID,
        LOJA_ENDERECO_LOJA_ID,
        USUARIO_ENDERECO_USUARIO_ID,
        USUARIO_ENDERECO_ENDERECO_ID,
        DISPOSITIVO_ACAO_ID,
        PAGO_EM_BONUS,
        USUARIO_TELEFONE_ID,
        valor_entrega_cobrado,
        valor_pagamento_cobrado,
        valor_acrescimo_cobrado,
        valor_troco,
        forma_entrega_id,
        forma_pagamento_id,
        IS_NOVO
        )
    VALUES (
        :loja_bonus_id,
        :loja_endereco_id,
        :LOJA_ID,
        :AUTOR_USUARIO_ID,
        :DESTINO_ENDERECO_ID_ALT,
        :dev_acao_id,
        :PEDIDO_PAGO_EM_BONUS,
        :tel_user_id,
        :VALOR_ENTREGA,
        null,
        null,
        :valor_troco,
        :forma_entrega_id,
        :forma_pagamento_id,
        1
    )
    RETURNING ID INTO :ID_PEDIDO;
    
    if (:OBS_USUARIO <> '') then
	begin
		INSERT INTO PEDIDO_OBSERVACAO (PEDIDO_ID, OBSERVACAO)
		VALUES (:ID_PEDIDO, :OBS_USUARIO);
	end
  SUSPEND;
END^
SET TERM ; ^
UPDATE RDB$PROCEDURES set
  RDB$DESCRIPTION = 'Efetua o registro de um pedido'
  where RDB$PROCEDURE_NAME = 'PR_REGISTRA_PEDIDO_REST';
GRANT EXECUTE
 ON PROCEDURE PR_REGISTRA_PEDIDO_REST TO  SYSDBA;




CREATE VIEW VW_INFOS_LOJA_FIREBASE (ID, CNPJ, NOME, LOJA_STATUS, IMAGEM,
    ENDERECO_ID, LOGRADOURO, NUMERO, COMPLEMENTO, REFERENCIA, BAIRRO, CIDADE,
    ESTADO, PAIS, CEP, LATITUDE, LONGITUDE, JSON_TMP_INFOS)
AS
SELECT DISTINCT
            r.ID,
            r.CNPJ,
            r.NOME,
            r.LOJA_STATUS,
            coalesce((SELECT FIRST 1 a1.APP_PATH FROM VW_ARQUIVO_LOJA_DISPONIVEL a1 WHERE a1.LOJA_ID = R.ID AND (a1.TIPO = 1 OR a1.TIPO = 2) ORDER BY a1.TIPO), 'not-found-img') as IMAGEM,
            e.ID as ENDERECO_ID,
            e.LOGRADOURO,
            e.NUMERO,
            e.COMPLEMENTO,
            e.REFERENCIA,
            e.BAIRRO,
            e.CIDADE,
            e.ESTADO,
            e.PAIS,
            e.CEP,
            e.LATITUDE,
            e.LONGITUDE,
            r.JSON_TMP_INFOS
        FROM
            LOJA r
        JOIN
            LOJA_ENDERECO le
                ON le.LOJA_ID = r.ID
        JOIN
            VW_INFOS_ENDERECO e
                ON e.ID = le.ENDERECO_ID
        WHERE
            r.LOJA_STATUS <> 0
            AND le.STATUS_ENDERECO = 1
            AND r.JSON_TMP_INFOS IS NOT NULL;















CREATE VIEW VW_LOJA_NOVO_FORMATO_FX (LOJA_ID)
AS
select
    r.LOJA_ID
from
    GRUPO_PRODUTO r
JOIN LOJA l
    ON l.ID = R.LOJA_ID
where
    exists(select 1 from PRODUTO where loja_id = r.LOJA_ID)
    and exists(select 1 from FORMA_ENTREGA where loja_id = r.LOJA_ID)
    and exists(select 1 from FORMA_PAGAMENTO where loja_id = r.LOJA_ID)
    AND l.JSON_TMP_INFOS IS NULL
group by
    1;

comment on view VW_LOJA_NOVO_FORMATO_FX is 'View temporária para retornar lojas que trabalham no novo formato de pedidos do fx';






create view VW_INFOS_PEDIDOS_PENDENTES_NOVO (ID, USUARIO_ID, USUARIO_NOME,
    USUARIO_CPF, USUARIO_TELEFONE_ID, USUARIO_TELEFONE_NUMERO,
    DESTINO_ENDERECO_ID, DESTINO_ENDERECO_LOGRADOURO, DESTINO_ENDERECO_NUMERO,
    DESTINO_ENDERECO_COMPLEMENTO, DESTINO_ENDERECO_REFERENCIA,
    DESTINO_ENDERECO_CEP, DESTINO_PAIS_NOME, DESTINO_ESTADO_NOME,
    DESTINO_ESTADO_SIGLA, DESTINO_CIDADE_NOME, DESTINO_CIDADE_IBGE,
    DESTINO_BAIRRO_NOME, TOTAL, HORARIO_ABERTURA, LOJA_ID, FORMA_ENTREGA_SC_ID,
    FORMA_PAGAMENTO_SC_ID, TROCO)
as
select
    p.ID,
    p.USUARIO_ENDERECO_USUARIO_ID as USUARIO_id,
    u.NOME_COMPLETO as usuario_nome,
    u.CPF as usuario_cpf,
    p.USUARIO_TELEFONE_ID as USUARIO_TELEFONE_id,
    ut.NUMERO as USUARIO_TELEFONE_numero,
    p.USUARIO_ENDERECO_ENDERECO_ID as destino_endereco_id,
    ed.LOGRADOURO as destino_endereco_logradouro,
    ed.NUMERO as destino_endereco_numero,
    ed.COMPLEMENTO as destino_endereco_complemento,
    ed.referencia as DESTINO_ENDERECO_REFERENCIA,
    ed.CEP as destino_endereco_cep,
    ed.PAIS as destino_pais_nome,
    uf.NOME as destino_estado_nome,
    uf.SIGLA as destino_estado_sigla,
    ed.CIDADE as destino_cidade_nome,
    ci.CODIGO_IBGE as destino_cidade_ibge,
    ed.BAIRRO as destino_bairro_nome,
    p.TOTAL,
    da.HORARIO as horario_abertura,
    p.LOJA_ENDERECO_LOJA_ID as loja_id,
    fe.BASE_CONTRATANTE_ID,
    fp.BASE_CONTRATANTE_ID,
    p.VALOR_TROCO
from PEDIDO p
inner join DISPOSITIVO_ACAO da
    on da.id = p.DISPOSITIVO_ACAO_ID
LEFT join USUARIO u
    on u.id = p.USUARIO_ENDERECO_USUARIO_ID and not EXISTS(
        select 1 from PEDIDO pp where pp.USUARIO_ENDERECO_USUARIO_ID = u.ID and pp.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null
    )
left join VW_INFOS_USUARIO_TELEFONE ut
    on ut.ID = p.USUARIO_TELEFONE_ID  and not EXISTS(
        select 1 from PEDIDO pp where pp.USUARIO_TELEFONE_ID = ut.ID and pp.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null
    )
left join VW_INFOS_ENDERECO ed
    on ed.ID = p.USUARIO_ENDERECO_ENDERECO_ID and not EXISTS(
        select 1 from PEDIDO pp where pp.USUARIO_ENDERECO_ENDERECO_ID = ed.ID and pp.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null
    )
left join estado uf
    on uf.ID = ed.ESTADO_ID
left join CIDADE ci
    on ci.ID = ed.CIDADE_ID
inner join LOJA l
    on l.ID = p.LOJA_ENDERECO_LOJA_ID
left join FORMA_ENTREGA fe
    on fe.ID = p.FORMA_ENTREGA_ID
left join FORMA_PAGAMENTO fp
    on fp.ID = p.FORMA_PAGAMENTO_ID
where
    p.MONITOR_SC_COLETOR_ACAO_ID is null
    and p.MONITOR_SC_FALHA_ACAO_ID is null
    and l.IS_FUNCIONAMENTO_FX = 1
    and p.IS_CANCELADO = 0
    and p.total_tmp is null
order by da.HORARIO asc ;


comment on view VW_INFOS_PEDIDOS_PENDENTES_NOVO is 'Retorna os pedidos pendentes para a loja no novo formato do fx';


CREATE VIEW VW_INFOS_PEDIDOS_ITENS_SC (PEDIDO_ID, ID, TAMANHO_ID, PRODUTO_ID,
    QUANTIDADE, OPCS, INGRS, SABS, SABS_INGR)
AS
with opcional as (
    select
        piop.PEDIDO_ITEM_ID,
        op.BASE_CONTRATANTE_ID || ',' || piop.VALOR_COBRADO as OPC
    from
        PEDIDO_ITEM_OPCIONAL_PROD piop
    inner join OPCIONAL_PRODUTO op
        on op.ID = piop.OPCIONAL_PRODUTO_ID
    ),
    ingrediente as (
        select
            pii.PEDIDO_ITEM_ID,
            p.BASE_CONTRATANTE_ID || ',' || pii.VALOR_COBRADO || ',' || pii.IS_REMOVIDO as INGR
        from
            PEDIDO_ITEM_INGR pii
        inner join PRODUTO p
            on p.ID = pii.PI_INGREDIENTE_ID
    ),
    sab as (
        select
            pisp.PEDIDO_ITEM_ID,
            s.BASE_CONTRATANTE_ID || ',' || pisp.VALOR_COBRADO as SAB
        from
            PEDIDO_ITEM_SABOR_PROD2 pisp
        inner join SABOR s
            on s.ID = pisp.SABOR_ID
    ),
    sab_ingrediente as (
        select
            pisp.PEDIDO_ITEM_ID as PEDIDO_ITEM_ID,
            s.BASE_CONTRATANTE_ID || ',' || p.BASE_CONTRATANTE_ID || ',' || pisi.VALOR_COBRADO || ',' || pisi.IS_REMOVIDO as SAB_INGR
        from
            PEDIDO_ITEM_SABOR_INGR2 pisi
        inner join PRODUTO p
            on p.ID = pisi.SBI_INGREDIENTE_ID
        inner join PEDIDO_ITEM_SABOR_PROD2 pisp
            ON pisp.ID = pisi.PIS_PEDIDO_SABOR_PROD_ID
        inner join SABOR s
            on s.ID = pisp.SABOR_ID
    )

select
    r.PEDIDO_ID,
    p.ID,
    tp.BASE_CONTRATANTE_ID as TAMANHO_ID,
    p.BASE_CONTRATANTE_ID as PRODUTO_ID,
    r.QUANTIDADE,
    list(distinct piop.OPC,';') as OPCS,
    list(distinct pii.INGR,';') as INGRS,
    list(distinct pisp.SAB,';') as SABS,
    list(distinct pisi.SAB_INGR,';') as SABS_INGR
from
    PEDIDO_ITEM r
inner join PRODUTO p
    on p.ID = r.PRODUTO_ID
left join TAMANHO_PRODUTO tp
    on tp.ID = r.TAMANHO_PRODUTO_ID
left join opcional piop
    on piop.PEDIDO_ITEM_ID = r.ID
left join ingrediente pii
    on pii.PEDIDO_ITEM_ID = r.ID
left join sab pisp
    on pisp.PEDIDO_ITEM_ID = r.ID
left join sab_ingrediente pisi
    on pisi.PEDIDO_ITEM_ID = r.ID
group by
    1, 2, 3, 4, 5;

comment on view VW_INFOS_PEDIDOS_ITENS_SC is 'Retorna os detalhes do itens de pedido no formado usado pelo sc';


CREATE VIEW VW_INFOS_PEDIDOS_ITENS_MONITO (PEDIDO_ID, TAMANHO, PRODUTO, TOTAL,
    QUANTIDADE, OPCS, INGRS, SABS)
AS   with opcional as (
    select
        piop.PEDIDO_ITEM_ID,
        op.DESCRICAO as OPC
    from
        PEDIDO_ITEM_OPCIONAL_PROD piop
    inner join OPCIONAL_PRODUTO op
        on op.ID = piop.OPCIONAL_PRODUTO_ID
    ),
    ingrediente as (
        select
            pii.PEDIDO_ITEM_ID,
            iif(pii.IS_REMOVIDO = 1, '+', '-') || p.DESCRICAO as INGR
        from
            PEDIDO_ITEM_INGR pii
        inner join PRODUTO p
            on p.ID = pii.PI_INGREDIENTE_ID
    ),
    sab as (
        select
            pisp.PEDIDO_ITEM_ID,
            s.DESCRICAO as SAB,
            list(iif(pisi.IS_REMOVIDO = 1, '+', '-') || p.DESCRICAO) as INGRS
        from
            PEDIDO_ITEM_SABOR_PROD2 pisp
        inner join SABOR s
            on s.ID = pisp.SABOR_ID
        left join PEDIDO_ITEM_SABOR_INGR2 pisi
            on pisi.PIS_PEDIDO_SABOR_PROD_ID = pisp.ID
        left join PRODUTO p
            on p.ID = pisi.SBI_INGREDIENTE_ID
        group by
            1, 2
    ),
    total_item as (
        select
            ID as PEDIDO_ITEM_ID,
            (select TOTAL from PR_VALOR_PEDIDO_ITEM(ID))
        from
            PEDIDO_ITEM
    )
select
    r.PEDIDO_ID,
    tp.DESCRICAO as TAMANHO,
    p.DESCRICAO as PRODUTO,
    r.QUANTIDADE,
    ti.total,
    list(distinct piop.OPC,';') as OPCS,
    list(distinct pii.INGR,';') as INGRS,
    list(distinct pisp.SAB || iif(pisp.INGRS is not null, ':' || pisp.INGRS, ''),';') as SABS
from
    PEDIDO_ITEM r
inner join PRODUTO p
    on p.ID = r.PRODUTO_ID
inner join total_item ti
    on ti.PEDIDO_ITEM_ID = r.ID
left join TAMANHO_PRODUTO tp
    on tp.ID = r.TAMANHO_PRODUTO_ID
left join opcional piop
    on piop.PEDIDO_ITEM_ID = r.ID
left join ingrediente pii
    on pii.PEDIDO_ITEM_ID = r.ID
left join sab pisp
    on pisp.PEDIDO_ITEM_ID = r.ID
group by
    1, 2, 3, 4, 5;

comment on view VW_INFOS_PEDIDOS_ITENS_MONITO is 'Retorna os detalhes de um item para ser usado na notificação de monitoramento';



create view VW_INFOS_PEDIDO_DETALHADO (ID, TOTAL, USUARIO_NOME, USUARIO_CPF,
    USUARIO_TELEFONE, LOJA_NOME, HORARIO_EMISSAO, HORARIO_COLETA,
    HORARIO_CONFIRMACAO, HORARIO_ESTIMADO_ENTREGA,
    HORARIO_RECEBIMENTO_NOTI_USER, IS_CANCELADO, FORMA_ENTREGA,
    FORMA_PAGAMENTO, VALOR_ACRESCIMO_COBRADO, VALOR_ENTREGA_COBRADO,
    VALOR_PAGAMENTO_COBRADO, VALOR_TROCO, ENDERECO_LOGRADOURO, ENDERECO_NUMERO,
    ENDERECO_BAIRRO, ENDERECO_CIDADE, ENDERECO_COMPLEMENTO,
    ENDERECO_REFERENCIA, ENDERECO_CEP, endereco_distancia)
as
with notis as (
    select
        max(da.HORARIO) as HORARIO,
        n.PEDIDO_ID
    from
        NOTIFICACAO n
    inner join NOTIFICACAO_USUARIO nu
        on nu.NOTIFICACAO_ID = n.ID
    inner join DISPOSITIVO_ACAO da
        on da.ID = nu.D_A_RECEBIMENTO_ID
    where
        n.TIPO <= 1
    group by
        2
)

select
    r.ID,
    r.TOTAL,
    u.NOME_COMPLETO,
    u.CPF,
    t.NUMERO,
    l.NOME,
    da.HORARIO,
    msa1.HORARIO,
    msa2.HORARIO,
    r.HORARIO_ESTIMADO_ENTREGA,
    n.horario,
    r.IS_CANCELADO,
    fe.DESCRICAO,
    iif(r.PAGO_EM_BONUS = 1,'Bônus',fp.DESCRICAO),
    r.VALOR_ACRESCIMO_COBRADO,
    r.VALOR_ENTREGA_COBRADO,
    r.VALOR_PAGAMENTO_COBRADO,
    r.VALOR_TROCO,
    e.LOGRADOURO,
    e.NUMERO,
    e.BAIRRO,
    e.CIDADE,
    e.COMPLEMENTO,
    e.REFERENCIA,
    e.CEP,
    r.DISTANCIA_LOJA_ENTREGA
from
    PEDIDO r
inner join USUARIO u
    on u.ID = r.USUARIO_ENDERECO_USUARIO_ID
inner join LOJA l
    on l.ID = r.LOJA_ENDERECO_LOJA_ID
inner join DISPOSITIVO_ACAO da
    on da.ID = r.DISPOSITIVO_ACAO_ID
inner join VW_INFOS_USUARIO_TELEFONE t
    on t.ID = r.USUARIO_TELEFONE_ID
left join MONITOR_SC_ACAO msa1
    on msa1.ID = r.MONITOR_SC_COLETOR_ACAO_ID
left join MONITOR_SC_ACAO msa2
    on msa2.ID = r.MONITOR_SC_CONFIRMADOR_ACAO_ID
left join FORMA_ENTREGA fe
    on fe.ID = r.FORMA_ENTREGA_ID
left join FORMA_PAGAMENTO fp
    on fp.ID = r.FORMA_PAGAMENTO_ID
left join VW_INFOS_ENDERECO e
    on e.ID = r.USUARIO_ENDERECO_ENDERECO_ID
    and r.USUARIO_ENDERECO_ENDERECO_ID <> r.LOJA_ENDERECO_ENDERECO_ID
left join notis n
    on n.PEDIDO_ID = r.ID;

comment on view vw_infos_pedido_detalhado is 'Retorna todos os detalhes a respeito do pedido';


CREATE VIEW VW_INFOS_PEDIDO_ITEM_DETALHADO (PEDIDO_ID, ID, PRODUTO, PRODUTO_ID, QTDE, VALOR_TOTAL, TAMANHO, OPCIONAIS_1, OPCIONAIS_2, INGREDIENTES, SABORES)
AS  
SELECT
    PI.PEDIDO_ID,
    PI.ID,
    PRO.DESCRICAO,
    PRO.ID,
    PI.QUANTIDADE,
    (COALESCE((SELECT TOTAL FROM PR_VALOR_PEDIDO_ITEM (PI.ID)),0) *PI.QUANTIDADE),
    TP.DESCRICAO,
    (
        SELECT LIST(DISTINCT OP.DESCRICAO, '<n>')
        FROM PEDIDO_ITEM_OPCIONAL_PROD PIOP
        INNER JOIN OPCIONAL_PRODUTO OP ON OP.ID = PIOP.OPCIONAL_PRODUTO_ID
        INNER JOIN OPCIONAL_PRODUTO_DISPONIVEL OPD ON OPD.OPCIONAL_PRODUTO_ID = OP.ID AND OPD.APP_TELA = 1
        WHERE PIOP.PEDIDO_ITEM_ID = PI.ID
        AND OPD.PRODUTO_ID = PI.PRODUTO_ID
    ),
    (
        SELECT LIST(DISTINCT OP.DESCRICAO, '<n>')
        FROM PEDIDO_ITEM_OPCIONAL_PROD PIOP
        INNER JOIN OPCIONAL_PRODUTO OP ON OP.ID = PIOP.OPCIONAL_PRODUTO_ID
        INNER JOIN OPCIONAL_PRODUTO_DISPONIVEL OPD ON OPD.OPCIONAL_PRODUTO_ID = OP.ID AND OPD.APP_TELA = 2
        WHERE PIOP.PEDIDO_ITEM_ID = PI.ID
        AND OPD.PRODUTO_ID = PI.PRODUTO_ID
    ),
    (
        SELECT LIST(IIF(PII.IS_REMOVIDO = 0, '<+>', '<->') || PRI.DESCRICAO || IIF(PII.IS_CORTESIA = 1, ' - cortesia', ''), '<n>')
        FROM PEDIDO_ITEM_INGR PII
        INNER JOIN PRODUTO PRI ON PRI.ID = PII.PI_INGREDIENTE_ID
        WHERE  PII.PEDIDO_ITEM_ID = PI.ID
    ),
    (
        SELECT LIST(SP.DESCRICAO ||COALESCE('<ings>' ||
            (
                SELECT LIST(IIF(PISI.IS_REMOVIDO = 0, '<+>', '<->') || PRI.DESCRICAO, '<n>')
                FROM PEDIDO_ITEM_SABOR_INGR2 PISI
                INNER JOIN PRODUTO PRI ON PRI.ID = PISI.SBI_INGREDIENTE_ID
                WHERE PISI.PIS_PEDIDO_SABOR_PROD_ID = PISP.ID
            ),''), '<nn>')
        FROM PEDIDO_ITEM_SABOR_PROD2 PISP
        INNER JOIN SABOR SP ON SP.ID = PISP.SABOR_ID
        WHERE PISP.PEDIDO_ITEM_ID = PI.ID
    )
FROM PEDIDO_ITEM PI
JOIN PRODUTO PRO ON PRO.ID = PI.PRODUTO_ID
LEFT JOIN TAMANHO_PRODUTO TP ON TP.ID = PI.TAMANHO_PRODUTO_ID
;
UPDATE RDB$RELATIONS set
  RDB$DESCRIPTION = 'Retorna todos os detalhes a respeito de cada item do pedido. As colunas de opcionais, ingredientes e sabores utilizam marcações como <n> para separar seus valores'
  where RDB$RELATION_NAME = 'VW_INFOS_PEDIDO_ITEM_DETALHADO';
GRANT SELECT
 ON VW_INFOS_PEDIDO_ITEM_DETALHADO TO  EGULAWEB;
GRANT SELECT
 ON VW_INFOS_PEDIDO_ITEM_DETALHADO TO  MONITORAMENTO;
GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE
 ON VW_INFOS_PEDIDO_ITEM_DETALHADO TO  SYSDBA WITH GRANT OPTION;







SET TERM ^ ;

CREATE PROCEDURE PR_REGISTRA_LOJA_ARQUIVO (
    loja_id int,
    imagem_id bigint,
    tipo DM_TIPO_ARQUIVO_LOJA
) AS
BEGIN


    if ((select count(1) from LOJA_ARQUIVO_DISPONIVEL
        where LOJA_ID = :LOJA_ID
            and ARQUIVO_DISPONIVEL_ID = :imagem_id
            and TIPO = :TIPO) > 0) then
    begin
        update LOJA_ARQUIVO_DISPONIVEL set
            TIPO = :TIPO
        where LOJA_ID = :LOJA_ID
            and ARQUIVO_DISPONIVEL_ID = :imagem_id;
    end
    else
    begin
        insert into LOJA_ARQUIVO_DISPONIVEL(LOJA_ID, ARQUIVO_DISPONIVEL_ID, TIPO)
            values (:LOJA_ID, :imagem_id, :TIPO);
    end



END^

SET TERM ; ^

comment on procedure PR_REGISTRA_LOJA_ARQUIVO is 'Vincula a imagem/arquivo ao cadastro da loja';







SET TERM ^ ;
create PROCEDURE PR_REGISTRA_LOJA_COMPLETO (
    CNPJ DM_CNPJ not null,
    NOME DM_NOME not null,
    LOJA_STATUS DM_LOJA_STATUS not null,
    PERCENTUAL_BONUS DM_PORCENTAGEM not null,
    LOGRADOURO DM_DESCRICAO  not null,
    NUMERO integer  not null,
    COMPLEMENTO DM_DESCRICAO,
    REFERENCIA DM_REFERENCIA,
    CEP DM_CEP,
    BAIRRO DM_NOME not null,
    CIDADE DM_NOME not null,
    ESTADO DM_NOME not null,
    PAIS DM_NOME not null,
    IMAGEM_APP_PATH_TIPO_1 varchar(150),
    IMAGEM_APP_PATH_TIPO_2 varchar(150),
    IMAGEM_APP_PATH_TIPO_3 varchar(150),
    IMAGEM_APP_PATH_TIPO_4 varchar(150)
) AS
declare variable id_endereco int;
declare variable id int;
BEGIN


    select r.ID
    from
        loja r
    where
        r.cnpj = :CNPJ
    into
        :id;


    if (:id is null) then
    begin

        INSERT INTO LOJA (CNPJ, NOME, LOJA_STATUS, APP_INDICE_APRESENTACAO,
            JSON_TMP_INFOS, IS_FUNCIONAMENTO_FX, LAST_UPDATE_INFOS)
            VALUES (
                :CNPJ,
                :NOME,
                :LOJA_STATUS,
                0,
                null,
                1,
                'now'
          ) RETURNING ID into :ID;

    end
    else
    begin
        UPDATE LOJA SET
            NOME = :NOME,
            LOJA_STATUS = :LOJA_STATUS,
            JSON_TMP_INFOS = null,
            IS_FUNCIONAMENTO_FX = 1,
            LAST_UPDATE_INFOS = 'now'
        WHERE
            ID = :ID
        ;
    end


    SELECT p.ID FROM PR_REGISTRA_ENDERECO (:LOGRADOURO, :NUMERO, :COMPLEMENTO, :REFERENCIA, :CEP, null, null,
         :BAIRRO, null, :CIDADE, null, :ESTADO, null, :PAIS, null) p
         into :id_endereco;


    if ((select count(0) from LOJA_ENDERECO
        where LOJA_ID = :id
            and ENDERECO_ID = :id_endereco) > 0) then
    begin
        if ((select count(0) from LOJA_ENDERECO
        where LOJA_ID = :id
            and ENDERECO_ID = :id_endereco
            AND STATUS_ENDERECO = 0) > 0) then
        begin
            UPDATE LOJA_ENDERECO SET STATUS_ENDERECO = 1 WHERE LOJA_ID = :id AND ENDERECO_ID = :id_endereco;
        end
    end
    else
    begin
        INSERT INTO LOJA_ENDERECO (ENDERECO_ID, STATUS_ENDERECO, LOJA_ID) VALUES (:id_endereco, 1, :ID);
    end



    if ((select count(0) from LOJA_BONUS_CONF
        where LOJA_ID = :id
            and PERCENTUAL = :PERCENTUAL_BONUS) > 0) then
    begin
        if ((select count(0) from LOJA_BONUS_CONF
        where LOJA_ID = :id
            and PERCENTUAL = :PERCENTUAL_BONUS
            AND STATUS_BONUS = 0) > 0) then
        begin
            UPDATE LOJA_BONUS_CONF SET STATUS_BONUS = 0 WHERE LOJA_ID = :id AND STATUS_BONUS = 1;
            UPDATE LOJA_BONUS_CONF SET STATUS_BONUS = 1 WHERE LOJA_ID = :id AND PERCENTUAL = :PERCENTUAL_BONUS;
        end
    end
    else
    begin
        UPDATE LOJA_BONUS_CONF SET STATUS_BONUS = 0 WHERE LOJA_ID = :id AND STATUS_BONUS = 1;
        INSERT INTO LOJA_BONUS_CONF (LOJA_ID, PERCENTUAL, STATUS_BONUS) VALUES (:ID,:PERCENTUAL_BONUS,1);
    end


    if (:IMAGEM_APP_PATH_TIPO_1 is not null and trim(:IMAGEM_APP_PATH_TIPO_1) != ''
        and :IMAGEM_APP_PATH_TIPO_1 != 'not-found-img') then
        execute procedure PR_REGISTRA_LOJA_ARQUIVO (:id, (select first 1 ID from ARQUIVO_DISPONIVEL where APP_PATH = :IMAGEM_APP_PATH_TIPO_1), 1);

    if (:IMAGEM_APP_PATH_TIPO_2 is not null and trim(:IMAGEM_APP_PATH_TIPO_2) != ''
        and :IMAGEM_APP_PATH_TIPO_2 != 'not-found-img') then
        execute procedure PR_REGISTRA_LOJA_ARQUIVO (:id, (select first 1 ID from ARQUIVO_DISPONIVEL where APP_PATH = :IMAGEM_APP_PATH_TIPO_2), 2);

    if (:IMAGEM_APP_PATH_TIPO_3 is not null and trim(:IMAGEM_APP_PATH_TIPO_3) != ''
        and :IMAGEM_APP_PATH_TIPO_3 != 'not-found-img') then
        execute procedure PR_REGISTRA_LOJA_ARQUIVO (:id, (select first 1 ID from ARQUIVO_DISPONIVEL where APP_PATH = :IMAGEM_APP_PATH_TIPO_3), 3);

    if (:IMAGEM_APP_PATH_TIPO_4 is not null and trim(:IMAGEM_APP_PATH_TIPO_4) != ''
        and :IMAGEM_APP_PATH_TIPO_4 != 'not-found-img') then
        execute procedure PR_REGISTRA_LOJA_ARQUIVO (:id, (select first 1 ID from ARQUIVO_DISPONIVEL where APP_PATH = :IMAGEM_APP_PATH_TIPO_4), 4);

END^
SET TERM ; ^

comment on procedure PR_REGISTRA_LOJA_COMPLETO is 'Efetua o cadastro completo de uma loja, caso ainda não exista tendo como base seu cnpj';

























alter table GRUPO_PRODUTO add ID_FORMATADO computed by ((select GRUPOMODULO from PR_FORMAT_GRUPO_MODULO (ID, 'GRUPO_PRODUTO', 'ID','GRUPO_PRODUTO_ID', 'CONTROLE_MODULO')));;

create view VWR_USUARIOS_SIMPLES (
    ID,
    NOME,
    TELEFONE,
    CIDADE
) as
SELECT
    u.ID,
    u.NOME_COMPLETO AS NOME,
    (SELECT first 1
         r.NUMERO
     FROM
         VW_INFOS_USUARIO_TELEFONE r
     where
         r.USUARIO_ID = u.ID
     order by
         r.TELEFONE_ID desc) AS TELEFONE,
    coalesce((SELECT first 1
         r.CIDADE
     FROM
         VW_INFOS_USUARIO_ENDERECO r
     where
         r.USUARIO_ID = u.id
     order by
         r.ID desc), 'Não encontrada') as CIDADE
FROM
    USUARIO u;

comment on view VWR_USUARIOS_SIMPLES is 'Lista de usuários simplificada';





SET TERM ^ ;
create PROCEDURE PR_REGISTRA_IMAGEM_LOJA (
    MD5 DM_MD5,
    TAMANHO bigint,
    PATH varchar(250),
    APP_PATH varchar(150),
    LOJA_id integer)
AS
DECLARE VARIABLE ID BIGINT;
BEGIN

    select id
    from ARQUIVO_DISPONIVEL ad
    where ad.PATH = trim(:path)
    into :id;



    if (:id is null) THEN
        INSERT INTO ARQUIVO_DISPONIVEL (MD5, TAMANHO, EXISTE, PATH, APP_PATH,
            HORARIO_CADASTRO) values (:md5, :tamanho, 1, trim(:path),
                :app_path, 'now') returning ID into :id;
    else
       update ARQUIVO_DISPONIVEL r set r.EXISTE = 1, r.MD5 = :MD5,
            r.TAMANHO = :TAMANHO
            where r.PATH = TRIM(:PATH)
                and (r.MD5 <> :MD5 or r.TAMANHO <> :TAMANHO);

    if (:LOJA_id is not null) THEN
    BEGIN
        if (not exists(
            select 1
            from LOJA_ARQUIVO_DISPONIVEL lad
            where
                lad.LOJA_ID = :LOJA_id
                and lad.ARQUIVO_DISPONIVEL_ID = :id
        )) then
            INSERT INTO LOJA_ARQUIVO_DISPONIVEL (LOJA_ID, ARQUIVO_DISPONIVEL_ID, TIPO)
                VALUES (:LOJA_id, :id, 0);

    end


END^
SET TERM ; ^

comment on procedure PR_REGISTRA_IMAGEM_LOJA is 'Cadastra/atualiza imagem na base';



CREATE VIEW VW_INFOS_PEDIDO_DETA_PEND_LOJA (ID, TOTAL, USUARIO_NOME,
    USUARIO_CPF, USUARIO_TELEFONE, LOJA_ID, HORARIO_EMISSAO, FORMA_ENTREGA,
    FORMA_PAGAMENTO, VALOR_ACRESCIMO_COBRADO, VALOR_ENTREGA_COBRADO,
    VALOR_PAGAMENTO_COBRADO, VALOR_TROCO, ENDERECO_LOGRADOURO, ENDERECO_NUMERO,
    ENDERECO_BAIRRO, ENDERECO_CIDADE, ENDERECO_COMPLEMENTO,
    ENDERECO_REFERENCIA, ENDERECO_CEP, ENDERECO_DISTANCIA, ENDERECO_TEMPO)
AS

select
    r.ID,
    r.TOTAL,
    u.NOME_COMPLETO,
    u.CPF,
    t.NUMERO,
    r.LOJA_ENDERECO_LOJA_ID,
    da.HORARIO,
    fe.DESCRICAO,
    iif(r.PAGO_EM_BONUS = 1,'Bônus',fp.DESCRICAO),
    r.VALOR_ACRESCIMO_COBRADO,
    r.VALOR_ENTREGA_COBRADO,
    r.VALOR_PAGAMENTO_COBRADO,
    r.VALOR_TROCO,
    e.LOGRADOURO,
    e.NUMERO,
    e.BAIRRO,
    e.CIDADE,
    e.COMPLEMENTO,
    e.REFERENCIA,
    e.CEP,
    r.DISTANCIA_LOJA_ENTREGA,
    (SELECT de.TEMPO FROM DISTANCIA_ENDERECO de WHERE de.ORIGEM_ENDERECO_ID = r.LOJA_ENDERECO_ENDERECO_ID AND de.DESTINO_ENDERECO_ID = r.USUARIO_ENDERECO_ENDERECO_ID)

from
    PEDIDO r
inner join USUARIO u
    on u.ID = r.USUARIO_ENDERECO_USUARIO_ID
inner join DISPOSITIVO_ACAO da
    on da.ID = r.DISPOSITIVO_ACAO_ID
inner join VW_INFOS_USUARIO_TELEFONE t
    on t.ID = r.USUARIO_TELEFONE_ID
left join FORMA_ENTREGA fe
    on fe.ID = r.FORMA_ENTREGA_ID
left join FORMA_PAGAMENTO fp
    on fp.ID = r.FORMA_PAGAMENTO_ID
left join VW_INFOS_ENDERECO e
    on e.ID = r.USUARIO_ENDERECO_ENDERECO_ID
    and r.USUARIO_ENDERECO_ENDERECO_ID <> r.LOJA_ENDERECO_ENDERECO_ID
where
    r.MONITOR_SC_CONFIRMADOR_ACAO_ID is null
    and r.MONITOR_SC_CANCELADOR_ACAO_ID is null
    and r.MONITOR_SC_FALHA_ACAO_ID is null
    and r.IS_CANCELADO = 0;



comment on view VW_INFOS_PEDIDO_DETA_PEND_LOJA is 'Pedido detalhado pendente para uso na tela de uma loja';



SET TERM ^ ;
create PROCEDURE PR_CONFIRMA_PEDIDO_WEB (
    ID bigint,
    MG_HOST_CLIENTE DM_HOST_PORT,
    PREVISAO_ENTREGA timestamp )
AS
declare variable acao_id bigint;
declare variable pago_em_bonus DM_BOOL;
BEGIN

    /**
    * Confirmação do pedido
    */


    if ((select count(0) from PEDIDO p where p.ID = :ID) = 0) then
    BEGIN
        EXCEPTION EXCEP_PEDIDO_NOT_FOUND;
    end


    if ((select count(0) from PEDIDO p where p.ID = :ID and p.MONITOR_SC_CONFIRMADOR_ACAO_ID is not null) > 0) then
    BEGIN
        EXCEPTION EXCEP_CONFIRMACAO_PED_DUPLICADA;
    end

    INSERT INTO MONITOR_SC_ACAO(HORARIO, HOST_CLIENT)
        values ('now',:MG_HOST_CLIENTE) RETURNING ID into :acao_id;

    update PEDIDO p set p.MONITOR_SC_CONFIRMADOR_ACAO_ID = :acao_id, p.HORARIO_ESTIMADO_ENTREGA = :PREVISAO_ENTREGA
        where p.ID = :ID;
    --Bugfix: caso o mg não tenha enviado ao capp o aviso de que recebeu o pedido
    update PEDIDO p set p.MONITOR_SC_COLETOR_ACAO_ID = :acao_id
        where p.ID = :ID AND p.MONITOR_SC_COLETOR_ACAO_ID is null;

END^
SET TERM ; ^
comment on procedure PR_CONFIRMA_PEDIDO_WEB is 'Efetua a confirmação de um pedido via web';




SET TERM ^ ;
create PROCEDURE PR_CANCELA_PEDIDO_WEB (
    ID bigint,
    MG_HOST_CLIENTE DM_HOST_PORT)
AS
declare variable acao_id bigint;
BEGIN

    /**
    * Cancelamento do pedido
    */


    if ((select count(0) from PEDIDO p where p.ID = :ID) = 0) then
    BEGIN
        EXCEPTION EXCEP_PEDIDO_NOT_FOUND;
    end


    if ((select count(0) from PEDIDO p where p.ID = :ID and p.MONITOR_SC_CANCELADOR_ACAO_ID is not null) > 0) then
    BEGIN
        EXCEPTION EXCEP_CONFIRMACAO_PED_DUPLICADA;
    end

    INSERT INTO MONITOR_SC_ACAO(HORARIO, HOST_CLIENT)
        values ('now',:MG_HOST_CLIENTE) RETURNING ID into :acao_id;

    update PEDIDO p set MONITOR_SC_CANCELADOR_ACAO_ID = :acao_id, IS_CANCELADO = 1
        where p.ID = :ID;
    --Bugfix: caso o mg não tenha enviado ao capp o aviso de que recebeu o pedido
    update PEDIDO p set p.MONITOR_SC_COLETOR_ACAO_ID = :acao_id
        where p.ID = :ID AND p.MONITOR_SC_COLETOR_ACAO_ID is null;

END^
SET TERM ; ^





comment on procedure PR_CANCELA_PEDIDO_WEB is 'Efetua o cancelamento de um pedido via web';














CREATE GENERATOR GEN_UNIDADE_MEDIDA_ID;

SET TERM !! ;
CREATE TRIGGER UNIDADE_MEDIDA_BI FOR UNIDADE_MEDIDA
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_UNIDADE_MEDIDA_ID, 1);
END!!
SET TERM ; !!








CREATE GENERATOR GEN_operador_ID;

SET TERM !! ;
CREATE TRIGGER operador_BI FOR operador
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_operador_ID, 1);
END!!
SET TERM ; !!








SET TERM ^ ;

CREATE PROCEDURE PR_VALIDA_LOGIN_MONITORA_NOVO (
    email DM_EMAIL,
    senha varchar(100)
) RETURNS (
    OK DM_BOOL
) AS
BEGIN
    ok = 0;

    if (exists(
            select 1
            from operador o
            where
                o.EMAIL = :email
                and o.SENHA = :senha
                and o.IS_LIBERADO_MONITORAMENTO = 1
        )) then ok = 1;

    suspend;

END^

SET TERM ; ^




SET TERM ^ ;

CREATE PROCEDURE PR_VALIDA_LOGIN_LOJA_WEB (
    email DM_EMAIL,
    senha varchar(100),
    loja_id int
) RETURNS (
    OK DM_BOOL
) AS
BEGIN
    ok = 0;

    /**
    * Ou você tem acesso a tudo
    */
    if (exists(
            select 1
            from operador o
            where
                o.EMAIL = :email
                and o.SENHA = :senha
                and o.IS_LIBERADO_FULL_WEB = 1
        )) then
    begin
        ok = 1;
    end
    else
    begin
        /**
        * Ou você tem acesso individual para com as lojas
        */
        if (exists(
            select 1
            from operador o
            inner join LOJA_OPERADOR_ACESSO loa
                on loa.LOJA_ID = :loja_id
                and loa.OPERADOR_ID = o.ID
            where
                o.EMAIL = :email
                and o.SENHA = :senha
        )) then ok = 1;
    end

    suspend;

END^

SET TERM ; ^





set term ^;

create trigger TPD_SPD_VALOR_BIU for TPD_SPD_VALOR
active before insert or update position 0
as
begin

    new.LAST_UPDATE = 'now';
end^

set term ;^




create view vwr_pedidos_historico_loja(
    id,
    horario_emissao,
    loja_id,
    usuario,
    total,
    status,
    horario_entrega,
    distancia
) as
select
    r.ID,
    da.horario as instante,
    r.LOJA_ENDERECO_LOJA_ID as loja_id,
    u.NOME_COMPLETO as usuario,
    r.TOTAL,
    case
        when r.is_cancelado = 1 then 2
        when r.HORARIO_ESTIMADO_ENTREGA is not null then 1
        else 0
    end  as status,
    r.HORARIO_ESTIMADO_ENTREGA as entrega,
    r.DISTANCIA_LOJA_ENTREGA as distancia
from
    PEDIDO r
inner join USUARIO u
    on u.ID = r.USUARIO_ENDERECO_USUARIO_ID
inner join DISPOSITIVO_ACAO da
    on da.ID = r.DISPOSITIVO_ACAO_ID
order by
    r.ID desc;

comment on view vwr_pedidos_historico_loja is 'Histórico de pedidos da loja, com informações resumidas';

set term ^;

create trigger LOJA_AIU for LOJA
active after insert or update position 0
as
begin


    if (
        new.LAST_UPDATE_INFOS is not null
        and exists(
            select 1
            from VW_LOJA_NOVO_FORMATO_FX l
            where l.LOJA_ID = new.ID
        )
    ) then
    post_event 'atualiza_loja';


end^

set term ;^






set term ^;

create trigger LOJA_ENDERECO_AIU for LOJA_ENDERECO
active after insert or update position 0
as
begin

    if (new.STATUS_ENDERECO = 1 and exists(
        select 1
        from ENDERECO e
        where
            e.ID = new.ENDERECO_ID
            and e.LATITUDE is null
            and e.LONGITUDE is null
    )) then
        post_event 'atualiza_endereco';

end^

set term ;^

set term ^;

create trigger ENDERECO_AU for ENDERECO
active after update position 0
as
begin

    if (new.LATITUDE is null and new.LONGITUDE is null and
    exists(
        select 1
        from LOJA_ENDERECO le
        where le.ENDERECO_ID = new.ID
            and le.STATUS_ENDERECO = 1
    )) then
     post_event 'atualiza_endereco';

end^

set term ;^



set term ^;

create trigger LOJA_AI for LOJA
active after insert position 0
as
begin
    post_event 'nova_loja';
end^

set term ;^



CREATE GENERATOR GEN_cliente_ID;

SET TERM !! ;
CREATE TRIGGER cliente_BI FOR cliente
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_cliente_ID, 1);
END!!
SET TERM ; !!





CREATE GENERATOR GEN_cliente_ID;

SET TERM !! ;
CREATE TRIGGER cliente_BI FOR cliente
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_cliente_ID, 1);
END!!
SET TERM ; !!




CREATE GENERATOR GEN_pedido_cliente_ID;

SET TERM !! ;
CREATE TRIGGER PEDIDO_CLIENTE_BI FOR PEDIDO_CLIENTE
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN

    NEW.ID = GEN_ID(GEN_pedido_cliente_ID, 1);

    new.HORARIO_EMISSAO = 'now';

    IF (NEW.HORARIO_AGENDAMENTO IS NULL) THEN
        NEW.HORARIO_AGENDAMENTO = 'NOW';

END!!
SET TERM ; !!



CREATE GENERATOR GEN_pedido_cliente_ITEM_ID;

SET TERM !! ;
CREATE TRIGGER PEDIDO_CLIENTE_ITEM_BI FOR PEDIDO_CLIENTE_ITEM
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_pedido_cliente_ITEM_ID, 1);
END!!
SET TERM ; !!



CREATE GENERATOR GEN_PEDIDO_CLIENTE_ITEM_S_BI_ID;

SET TERM !! ;
CREATE TRIGGER PEDIDO_CLIENTE_ITEM_SABOR_BI FOR PEDIDO_CLIENTE_ITEM_SABOR
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_PEDIDO_CLIENTE_ITEM_S_BI_ID, 1);
END!!
SET TERM ; !!




SET TERM ^ ;
CREATE PROCEDURE PR_VALOR_PEDIDO_CLIENTE_ITEM (
    PEDIDO_CLIENTE_ITEM_ID integer )
RETURNS (
    TOTAL DM_MONEY )
AS
declare VARIABLE tmp DM_MONEY;
BEGIN

    tmp = 0;

    select
        coalesce(pi.VALOR_UNIDADE_COBRADO, 0)
    from
        PEDIDO_CLIENTE_ITEM pi
    where
        pi.ID = :PEDIDO_CLIENTE_ITEM_ID
    into tmp;

    TOTAL = coalesce(:tmp, 0);

    select
        sum(coalesce(pii.VALOR_COBRADO, 0))
    from
        PEDIDO_CLIENTE_ITEM_INGR pii
    where
        pii.PEDIDO_CLIENTE_ITEM_ID = :PEDIDO_CLIENTE_ITEM_ID
    into tmp;

   TOTAL = coalesce(:tmp, 0) + :total;

    select
        sum(coalesce(piop.VALOR_COBRADO, 0))
    from
        PEDIDO_CLIENTE_ITEM_OPC piop
    where
        piop.PEDIDO_CLIENTE_ITEM_ID = :PEDIDO_CLIENTE_ITEM_ID
    into tmp;

   TOTAL = coalesce(:tmp, 0) + :total;


    select
       sum( coalesce(pisp.VALOR_COBRADO, 0))
    from
        PEDIDO_CLIENTE_ITEM_SABOR pisp
    where
        pisp.PEDIDO_CLIENTE_ITEM_ID = :PEDIDO_CLIENTE_ITEM_ID
    into tmp;

    TOTAL = coalesce(:tmp, 0) + :total;

    select
        sum(coalesce(pisi.VALOR_COBRADO, 0))
    from
         PEDIDO_CLIENTE_ITEM_SAB_INGRE pisi
    inner join PEDIDO_CLIENTE_ITEM_SABOR pisp
        on pisp.ID = pisi.PCIS_SABOR_ID
    where
        pisp.PEDIDO_CLIENTE_ITEM_ID = :PEDIDO_CLIENTE_ITEM_ID
    into tmp;

    TOTAL = coalesce(:tmp, 0) + :total;

    suspend;
END^
SET TERM ; ^



SET TERM ^ ;
CREATE PROCEDURE PR_VALOR_TOTAL_PEDIDO_CLIENTE (
    PEDIDO_CLIENTE_ID integer )
RETURNS (
    VALOR_TOTAL DM_MONEY )
AS
BEGIN
    select
        coalesce(
            sum(
                (SELECT coalesce(TOTAL, 0) FROM PR_VALOR_PEDIDO_CLIENTE_ITEM (pi.ID)
            ) * pi.QUANTIDADE
        ), 0) as valor_total
    from
        PEDIDO_CLIENTE p
    join
        PEDIDO_CLIENTE_ITEM pi on pi.PEDIDO_CLIENTE_ID = p.ID
    where
        p.ID = :PEDIDO_CLIENTE_ID
    into VALOR_TOTAL;

    suspend;
END^
SET TERM ; ^

alter table PEDIDO_CLIENTE add total computed by ((select VALOR_TOTAL from PR_VALOR_TOTAL_PEDIDO_CLIENTE (id)));

SET TERM ^;


CREATE GENERATOR GEN_GARCOM_ID;
SET GENERATOR GEN_GARCOM_ID TO 0;

CREATE TRIGGER GARCOM_BI FOR GARCOM
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_GARCOM_ID, 1);
END^
SET TERM ;^


SET TERM ^ ;
CREATE PROCEDURE PR_ALTERA_USUARIO (
    USUARIO_ID INTEGER,
    NOME DM_NOME,
    SOBRENOME DM_SOBRENOME,
    EMAIL DM_EMAIL,
    TELEFONE_NUMERO DM_TELEFONE )
AS
DECLARE VARIABLE TELEFONE_NUMERO_EXISTENTE DM_TELEFONE;
    DECLARE VARIABLE ID_TEL_USUARIO BIGINT;
    DECLARE VARIABLE ID_TEL_NOVO BIGINT;

BEGIN

    UPDATE
        USUARIO a
    SET
        a.NOME = :NOME,
        a.SOBRENOME = :SOBRENOME,
        a.EMAIL = :EMAIL
    WHERE
        a.ID = :USUARIO_ID;

    SELECT FIRST 1
        ut.ID
    FROM
        USUARIO_TELEFONE ut
    WHERE
        ut.USUARIO_ID = :USUARIO_ID
    ORDER BY
        ut.ID DESC
    INTO ID_TEL_USUARIO;

    IF (
        NOT EXISTS (
            SELECT
                ut.ID
            FROM
                USUARIO_TELEFONE ut
            WHERE
                ut.ID = :ID_TEL_USUARIO
                AND ut.TELEFONE_ID IN (SELECT t.ID FROM TELEFONE t WHERE t.NUMERO = :TELEFONE_NUMERO)
        )
    ) THEN
        IN AUTONOMOUS TRANSACTION DO
            BEGIN
                INSERT INTO TELEFONE (NUMERO) VALUES (:TELEFONE_NUMERO) RETURNING ID INTO ID_TEL_NOVO;
                INSERT INTO USUARIO_TELEFONE (USUARIO_ID, TELEFONE_ID) VALUES (:USUARIO_ID, :ID_TEL_NOVO);
            END

END^
SET TERM ; ^


GRANT EXECUTE
 ON PROCEDURE PR_ALTERA_USUARIO TO  SYSDBA;

CREATE GENERATOR GEN_LOJA_CONFIG_EGULA_WEB_ID;
SET GENERATOR GEN_LOJA_CONFIG_EGULA_WEB_ID TO 0;

SET TERM ^ ;
CREATE TRIGGER LOJA_CONFIG_EGULA_WEB_BI FOR LOJA_CONFIG_EGULA_WEB ACTIVE
BEFORE INSERT POSITION 0
AS BEGIN NEW.ID = GEN_ID(GEN_LOJA_CONFIG_EGULA_WEB_ID, 1); END^
SET TERM ; ^


CREATE GENERATOR GEN_DISTANCIA_ENDERECO_ID;
SET GENERATOR GEN_DISTANCIA_ENDERECO_ID TO 0;

SET TERM ^ ;
CREATE TRIGGER DISTANCIA_ENDERECO_BI
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_DISTANCIA_ENDERECO_ID, 1);
END^
SET TERM ; ^

CREATE GENERATOR GEN_MESA_ABERTA_ID;

SET TERM !! ;
CREATE TRIGGER MESA_ABERTA_BI FOR MESA_ABERTA
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_MESA_ABERTA_ID, 1);
END!!
SET TERM ; !!


CREATE GENERATOR GEN_MESA_FECHAMENTO_ID;

SET TERM !! ;
CREATE TRIGGER MESA_FECHAMENTO_BI FOR MESA_FECHAMENTO
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_MESA_FECHAMENTO_ID, 1);
END!!
SET TERM ; !!


CREATE GENERATOR GEN_MESA_FECHAMENTO_ITEM_ID;

SET TERM !! ;
CREATE TRIGGER MESA_FECHAMENTO_ITEM_BI FOR MESA_FECHAMENTO_PARCIAL_ITEM
ACTIVE BEFORE INSERT POSITION 0
AS
BEGIN
    NEW.ID = GEN_ID(GEN_MESA_FECHAMENTO_ITEM_ID, 1);
END!!
SET TERM ; !!

