CREATE TABLE pedido_cliente (
  id BIGINT  NOT NULL  ,
  horario_emissao TIMESTAMP  NOT NULL  ,
  horario_agendamento TIMESTAMP  NOT NULL  ,
  horario_finalizacao TIMESTAMP    ,
  observacoes DM_TEXTO    ,
  is_cancelado TIMESTAMP      ,
PRIMARY KEY(id));



CREATE TABLE conf_egula_web (
  tempo_recarregamento_pedidos SMALLINT  NOT NULL  );



CREATE TABLE servidor (
  id INTEGER  NOT NULL  ,
  host dm_host_port  NOT NULL  ,
  is_master dm_bool  NOT NULL  ,
  descricao dm_descricao  NOT NULL    ,
PRIMARY KEY(id));



CREATE TABLE pais (
  id INTEGER  NOT NULL  ,
  nome dm_nome  NOT NULL    ,
PRIMARY KEY(id));



CREATE TABLE garcom (
  id INTEGER  NOT NULL  ,
  nome dm_nome  NOT NULL  ,
  sobrenome dm_sobrenome  NOT NULL  ,
  cpf dm_cpf  NOT NULL  ,
  email DM_EMAIL  NOT NULL  ,
  senha VARCHAR(100)  NOT NULL    ,
PRIMARY KEY(id)  );


CREATE INDEX garcom_unique ON garcom (cpf, email);



CREATE TABLE loja (
  id INTEGER  NOT NULL  ,
  cnpj dm_cnpj  NOT NULL  ,
  nome dm_nome  NOT NULL  ,
  loja_status dm_loja_status  NOT NULL  ,
  app_indice_apresentacao INTEGER  NOT NULL  ,
  json_tmp_infos VARCHAR(5000)    ,
  is_funcionamento_fx dm_bool    ,
  last_update_infos TIMESTAMP    ,
  envia_cpf dm_bool    ,
  last_update_infos_bloqueada TIMESTAMP    ,
  divulgacao_status dm_bool  NOT NULL DEFAULT 0   ,
PRIMARY KEY(id)  );


CREATE UNIQUE INDEX loja_unique ON loja (cnpj);

COMMENT ON COLUMN loja.loja_status IS '0 => Indisponivel | 1 => Habilitado | 2 => Modo Testes | 3 => Restrito Autorizaçao | 4 => Modo Garçom | 5 => Garçom em Testes';
COMMENT ON COLUMN loja.json_tmp_infos IS 'Temporario';
COMMENT ON COLUMN loja.is_funcionamento_fx IS 'Determina se esta trabalhando com os processo adaptados para o fx. Temporario. Em processo de migração';
COMMENT ON COLUMN loja.last_update_infos_bloqueada IS '!null => a última alteraçúo/atualizaçao de conteúdo que a loja enviou foi negada e nao sera disponibilizada';
COMMENT ON COLUMN loja.divulgacao_status IS '1 => se apresentara no app em modo divulgaçao, sobrepondo a regra da loja_status';


CREATE TABLE operador (
  id INTEGER  NOT NULL  ,
  nome dm_nome  NOT NULL  ,
  email DM_EMAIL  NOT NULL  ,
  senha VARCHAR(100)  NOT NULL  ,
  is_liberado_monitoramento dm_bool  NOT NULL DEFAULT 0 ,
  is_liberado_full_web dm_bool  NOT NULL DEFAULT 0   ,
PRIMARY KEY(id)  );


CREATE UNIQUE INDEX operador_unique ON operador (email);

COMMENT ON COLUMN operador.is_liberado_monitoramento IS 'Se tem acesso ao monitoramento web';
COMMENT ON COLUMN operador.is_liberado_full_web IS 'Se tem acesso a todas as lojas no egula web';


CREATE TABLE unidade_medida (
  id INTEGER  NOT NULL  ,
  descricao dm_nome  NOT NULL  ,
  sigla CHAR(2)  NOT NULL    ,
PRIMARY KEY(id)  );


CREATE UNIQUE INDEX unidade_medida_unique ON unidade_medida (sigla);



CREATE TABLE categoria (
  id INTEGER  NOT NULL  ,
  descricao dm_descricao      ,
PRIMARY KEY(id));



-- ------------------------------------------------------------
-- Tabela para registro das consultas realizadas em apis externas como google maps e fcm
-- ------------------------------------------------------------

CREATE TABLE uso_api_externa (
  id BIGINT  NOT NULL  ,
  horario_consulta TIMESTAMP  NOT NULL  ,
  api_usada DM_TIPO_API  NOT NULL  ,
  conclusao_pendente dm_bool  NOT NULL    ,
PRIMARY KEY(id));

COMMENT ON TABLE uso_api_externa IS 'Tabela para registro das consultas realizadas em apis externas como google maps e fcm';
COMMENT ON COLUMN uso_api_externa.conclusao_pendente IS 'Se existe algo pendente nessa requisiçao. Usado no fcm para saber se o dispositivo comunicou depois de ser disparada a requisiçao, servindo assim para evitar multiplas requisicoes desnecessarias a api';


CREATE TABLE telefone (
  id BIGINT  NOT NULL  ,
  numero dm_telefone  NOT NULL  ,
  operadora VARCHAR(25)      ,
PRIMARY KEY(id));



CREATE TABLE arquivo_disponivel (
  id BIGINT  NOT NULL  ,
  path VARCHAR(250)  NOT NULL  ,
  md5 dm_md5  NOT NULL  ,
  tamanho BIGINT  NOT NULL  ,
  app_path VARCHAR(150)  NOT NULL  ,
  existe dm_bool  NOT NULL  ,
  horario_cadastro TIMESTAMP  NOT NULL    ,
PRIMARY KEY(id));

COMMENT ON COLUMN arquivo_disponivel.tamanho IS 'Em bytes';
COMMENT ON COLUMN arquivo_disponivel.app_path IS 'Com se chama no app';
COMMENT ON COLUMN arquivo_disponivel.existe IS '0 => Nao | 1 => Sim';


CREATE TABLE loja_bonus_conf (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  percentual dm_porcentagem  NOT NULL  ,
  status_bonus dm_uso_status  NOT NULL    ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX loja_bonus_conf_FKIndex1 ON loja_bonus_conf (loja_id);

COMMENT ON COLUMN loja_bonus_conf.status_bonus IS '1 => Em uso | 0 => Sem uso';


CREATE TABLE loja_config_egula_web (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER    ,
  data_hora_agenda SMALLINT    ,
  tam_fonte_imp_pedido INTEGER      ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX loja_config_egula_web_FKIndex1 ON loja_config_egula_web (loja_id);

COMMENT ON COLUMN loja_config_egula_web.data_hora_agenda IS '0 = Data/Hora | 1 = Tempo de entrega';
COMMENT ON COLUMN loja_config_egula_web.tam_fonte_imp_pedido IS 'Tamanho da fonte para impressão';


CREATE TABLE horario_atendimento (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  dia_semana SMALLINT    ,
  data_exata DATE    ,
  hora_inicio TIME  NOT NULL  ,
  hora_fim TIME  NOT NULL  ,
  valor_acrescimo dm_money    ,
  app_status dm_bool  NOT NULL  ,
  tipo_atendimento SMALLINT  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER      ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX horario_atendimento_FKIndex1 ON horario_atendimento (loja_id);



-- ------------------------------------------------------------
-- Controle de creditos liberados a loja em questao
-- ------------------------------------------------------------

CREATE TABLE loja_credito (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  cadastro DATE  NOT NULL  ,
  valor dm_money  NOT NULL  ,
  credito dm_money  NOT NULL    ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX loja_credito_FKIndex1 ON loja_credito (loja_id);

COMMENT ON TABLE loja_credito IS 'Controle de creditos liberados a loja em questao';
COMMENT ON COLUMN loja_credito.valor IS 'Valor cobrado do contratante';
COMMENT ON COLUMN loja_credito.credito IS 'Total R$ em pedidos que a loja recebeu de credito, ou seja, ela pode vender esse montante';


CREATE TABLE opcional_produto (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  descricao dm_descricao  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER      ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX opcional_produto_FKIndex1 ON opcional_produto (loja_id);



CREATE TABLE sabor (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  descricao dm_descricao  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER      ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX sabor_FKIndex1 ON sabor (loja_id);



CREATE TABLE tamanho_produto (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  descricao dm_descricao  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER      ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX tamanho_produto_FKIndex1 ON tamanho_produto (loja_id);



CREATE TABLE mesa_aberta (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  mesa VARCHAR(5)  NOT NULL  ,
  stats INTEGER  NOT NULL    ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX mesa_aberta_FKIndex1 ON mesa_aberta (loja_id);



CREATE TABLE monitor_sc (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  mac dm_mac  NOT NULL    ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX monitor_sc_FKIndex1 ON monitor_sc (loja_id);



CREATE TABLE monitor_sc_acao (
  id BIGINT  NOT NULL  ,
  monitor_sc_id INTEGER    ,
  host_client dm_host_port  NOT NULL  ,
  horario TIMESTAMP  NOT NULL    ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(monitor_sc_id)
    REFERENCES monitor_sc(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX monitor_sc_acao_FKIndex1 ON monitor_sc_acao (monitor_sc_id);

COMMENT ON COLUMN monitor_sc_acao.monitor_sc_id IS 'null => foi feito por meio do navegador/interfaceweb';


CREATE TABLE dispositivo (
  id BIGINT  NOT NULL  ,
  telefone_id BIGINT    ,
  id_hardware VARCHAR(100)  NOT NULL  ,
  horario_cadastro TIMESTAMP  NOT NULL  ,
  so dm_so_dispositivo  NOT NULL  ,
  modelo VARCHAR(100)    ,
  fabricante VARCHAR(100)    ,
  so_versao VARCHAR(10)    ,
  sdk_versao VARCHAR(30)    ,
  force_logoff_pendente dm_bool  NOT NULL    ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(telefone_id)
    REFERENCES telefone(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE UNIQUE INDEX dispositivo_unique ON dispositivo (id_hardware);
CREATE INDEX dispositivo_FKIndex1 ON dispositivo (telefone_id);

COMMENT ON COLUMN dispositivo.so IS '0 => Android | 1 = iOS';
COMMENT ON COLUMN dispositivo.force_logoff_pendente IS 'Se existe um processo de forçar o logoff pendente para esse dispositivo';


CREATE TABLE forma_pagamento (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  descricao dm_nome  NOT NULL  ,
  is_com_troco dm_bool  NOT NULL  ,
  app_status dm_bool  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER    ,
  is_bonus dm_bool  NOT NULL DEFAULT 0   ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX forma_pagamento_FKIndex1 ON forma_pagamento (loja_id);



CREATE TABLE dispositivo_acao (
  id BIGINT  NOT NULL  ,
  dispositivo_id BIGINT  NOT NULL  ,
  host_cliente dm_host_port  NOT NULL  ,
  horario TIMESTAMP  NOT NULL    ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(dispositivo_id)
    REFERENCES dispositivo(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX dispositivo_acao_FKIndex1 ON dispositivo_acao (dispositivo_id);



CREATE TABLE forma_entrega (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  descricao dm_nome  NOT NULL  ,
  app_status dm_bool  NOT NULL  ,
  app_is_local dm_bool  NOT NULL  ,
  taxa_fixa dm_money    ,
  app_show_forma_pag dm_bool  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER      ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX forma_entrega_FKIndex1 ON forma_entrega (loja_id);



CREATE TABLE estado (
  id INTEGER  NOT NULL  ,
  pais_id INTEGER  NOT NULL  ,
  nome dm_nome  NOT NULL  ,
  sigla CHAR(2)  NOT NULL    ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(pais_id)
    REFERENCES pais(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX estado_FKIndex1 ON estado (pais_id);



-- ------------------------------------------------------------
-- Cadastro de clientes
-- ------------------------------------------------------------

CREATE TABLE cliente (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  telefone_id BIGINT    ,
  nome dm_nome  NOT NULL  ,
  email DM_EMAIL      ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(telefone_id)
    REFERENCES telefone(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX cliente_FKIndex1 ON cliente (telefone_id);
CREATE INDEX cliente_FKIndex2 ON cliente (loja_id);

COMMENT ON TABLE cliente IS 'Cadastro de clientes';


CREATE TABLE pedido_cliente_garcom (
  pedido_cliente_id BIGINT  NOT NULL  ,
  garcom_id INTEGER  NOT NULL  ,
  mesa INTEGER    ,
  comanda INTEGER      ,
PRIMARY KEY(pedido_cliente_id)    ,
  FOREIGN KEY(pedido_cliente_id)
    REFERENCES pedido_cliente(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(garcom_id)
    REFERENCES garcom(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX pedido_cliente_g_FKIndex1 ON pedido_cliente_garcom (pedido_cliente_id);
CREATE INDEX pedido_cliente_g_FKIndex2 ON pedido_cliente_garcom (garcom_id);



CREATE TABLE loja_servidor_default (
  loja_id INTEGER  NOT NULL  ,
  servidor_id INTEGER  NOT NULL  ,
  indice SMALLINT  NOT NULL    ,
PRIMARY KEY(loja_id, servidor_id)    ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(servidor_id)
    REFERENCES servidor(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX loja_s_FKIndex1 ON loja_servidor_default (loja_id);
CREATE INDEX loja_s_FKIndex2 ON loja_servidor_default (servidor_id);

COMMENT ON COLUMN loja_servidor_default.indice IS 'Ordenação da comunicação';


-- ------------------------------------------------------------
-- Quando o operador tem acesso apenas a algumas lojas
-- ------------------------------------------------------------

CREATE TABLE loja_operador_acesso (
  loja_id INTEGER  NOT NULL  ,
  operador_id INTEGER  NOT NULL    ,
PRIMARY KEY(loja_id, operador_id)    ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(operador_id)
    REFERENCES operador(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX loja_operador_FKIndex1 ON loja_operador_acesso (loja_id);
CREATE INDEX loja_operador_FKIndex2 ON loja_operador_acesso (operador_id);

COMMENT ON TABLE loja_operador_acesso IS 'Quando o operador tem acesso apenas a algumas lojas';


CREATE TABLE mesa_pedido (
  mesa_aberta_id INTEGER  NOT NULL  ,
  pedido_cliente_id BIGINT  NOT NULL    ,
PRIMARY KEY(mesa_aberta_id, pedido_cliente_id)    ,
  FOREIGN KEY(mesa_aberta_id)
    REFERENCES mesa_aberta(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(pedido_cliente_id)
    REFERENCES pedido_cliente(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX mesa_aberta_has_pedido_cliente_FKIndex1 ON mesa_pedido (mesa_aberta_id);
CREATE INDEX mesa_aberta_has_pedido_cliente_FKIndex2 ON mesa_pedido (pedido_cliente_id);



CREATE TABLE mesa_fechamento (
  id INTEGER  NOT NULL  ,
  forma_pagamento_id INTEGER  NOT NULL  ,
  mesa_aberta_id INTEGER  NOT NULL  ,
  hora TIMESTAMP  NOT NULL  ,
  valor_desconto dm_money  NOT NULL  ,
  valor_acrescimo dm_money  NOT NULL  ,
  valor_troco dm_money  NOT NULL    ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(mesa_aberta_id)
    REFERENCES mesa_aberta(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(forma_pagamento_id)
    REFERENCES forma_pagamento(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX mesa_fechamento_FKIndex1 ON mesa_fechamento (mesa_aberta_id);
CREATE INDEX mesa_fechamento_FKIndex2 ON mesa_fechamento (forma_pagamento_id);



CREATE TABLE loja_categoria (
  loja_id INTEGER  NOT NULL  ,
  categoria_id INTEGER  NOT NULL    ,
PRIMARY KEY(loja_id, categoria_id)    ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(categoria_id)
    REFERENCES categoria(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX loja_has_categoria_FKIndex1 ON loja_categoria (loja_id);
CREATE INDEX loja_has_categoria_FKIndex2 ON loja_categoria (categoria_id);



CREATE TABLE loja_arquivo_disponivel (
  loja_id INTEGER  NOT NULL  ,
  arquivo_disponivel_id BIGINT  NOT NULL  ,
  tipo DM_TIPO_ARQUIVO_LOJA  NOT NULL  ,
  is_em_uso dm_bool      ,
PRIMARY KEY(loja_id, arquivo_disponivel_id)    ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(arquivo_disponivel_id)
    REFERENCES arquivo_disponivel(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX loja_has_a_FKIndex1 ON loja_arquivo_disponivel (loja_id);
CREATE INDEX loja_hasa_FKIndex2 ON loja_arquivo_disponivel (arquivo_disponivel_id);



CREATE TABLE grupo_produto (
  id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  arquivo_disponivel_id BIGINT    ,
  grupo_produto_id INTEGER  NOT NULL  ,
  controle_modulo INTEGER  NOT NULL  ,
  descricao dm_descricao  NOT NULL  ,
  app_status dm_bool  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER      ,
PRIMARY KEY(id)      ,
  FOREIGN KEY(grupo_produto_id)
    REFERENCES grupo_produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(arquivo_disponivel_id)
    REFERENCES arquivo_disponivel(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX grupo_produto_FKIndex1 ON grupo_produto (grupo_produto_id);
CREATE INDEX grupo_produto_FKIndex2 ON grupo_produto (arquivo_disponivel_id);
CREATE INDEX grupo_produto_FKIndex3 ON grupo_produto (loja_id);



CREATE TABLE loja_garcom (
  loja_id INTEGER  NOT NULL  ,
  garcom_id INTEGER  NOT NULL    ,
PRIMARY KEY(loja_id, garcom_id)    ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(garcom_id)
    REFERENCES garcom(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX lg_FKIndex1 ON loja_garcom (loja_id);
CREATE INDEX lg_FKIndex2 ON loja_garcom (garcom_id);



CREATE TABLE distancia_entrega (
  id INTEGER  NOT NULL  ,
  forma_entrega_id INTEGER  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  distancia_min DECIMAL(3,1)  NOT NULL  ,
  distancia_max DECIMAL(3,1)  NOT NULL  ,
  valor dm_money  NOT NULL DEFAULT 0   ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(forma_entrega_id)
    REFERENCES forma_entrega(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX de_FKIndex1 ON distancia_entrega (loja_id);
CREATE INDEX de_FKIndex2 ON distancia_entrega (forma_entrega_id);



CREATE TABLE dispositivo_arquivo_disponivel (
  dispositivo_id BIGINT  NOT NULL  ,
  arquivo_disponivel_id BIGINT  NOT NULL    ,
PRIMARY KEY(dispositivo_id, arquivo_disponivel_id)    ,
  FOREIGN KEY(dispositivo_id)
    REFERENCES dispositivo(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(arquivo_disponivel_id)
    REFERENCES arquivo_disponivel(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX dispositivo_a_FKIndex1 ON dispositivo_arquivo_disponivel (dispositivo_id);
CREATE INDEX dispositivo_al_FKIndex2 ON dispositivo_arquivo_disponivel (arquivo_disponivel_id);



-- ------------------------------------------------------------
-- Relaçao de hosts default por dispositivo 
-- ------------------------------------------------------------

CREATE TABLE dispositivo_servidor_default (
  servidor_id INTEGER  NOT NULL  ,
  dispositivo_id BIGINT  NOT NULL  ,
  indice SMALLINT  NOT NULL    ,
PRIMARY KEY(servidor_id, dispositivo_id)    ,
  FOREIGN KEY(servidor_id)
    REFERENCES servidor(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(dispositivo_id)
    REFERENCES dispositivo(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX dispositivo_s_FKIndex2 ON dispositivo_servidor_default (servidor_id);
CREATE INDEX dispositivo_s_FKIndex3 ON dispositivo_servidor_default (dispositivo_id);

COMMENT ON TABLE dispositivo_servidor_default IS 'Relaçao de hosts default por dispositivo ';
COMMENT ON COLUMN dispositivo_servidor_default.indice IS 'Ordenacao para comunicacao';


CREATE TABLE arquivo_download (
  arquivo_disponivel_id BIGINT  NOT NULL  ,
  dispositivo_acao_id BIGINT  NOT NULL  ,
  exclusao_dev_acao_id BIGINT    ,
  horario_fim_download TIMESTAMP      ,
PRIMARY KEY(arquivo_disponivel_id, dispositivo_acao_id)      ,
  FOREIGN KEY(arquivo_disponivel_id)
    REFERENCES arquivo_disponivel(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(dispositivo_acao_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(exclusao_dev_acao_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX arquivo_download_FKIndex1 ON arquivo_download (arquivo_disponivel_id);
CREATE INDEX arquivo_download_FKIndex2 ON arquivo_download (dispositivo_acao_id);
CREATE INDEX arquivo_download_FKIndex3 ON arquivo_download (exclusao_dev_acao_id);

COMMENT ON COLUMN arquivo_download.dispositivo_acao_id IS 'Nos prove host + horario inicio de download';
COMMENT ON COLUMN arquivo_download.exclusao_dev_acao_id IS 'Nos prove host + horario da exclusao do arquivo no app. ';


CREATE TABLE garcom_logado (
  garcom_id INTEGER  NOT NULL  ,
  dispositivo_acao_login_id BIGINT  NOT NULL  ,
  dispositivo_acao_logoff_id BIGINT  NOT NULL    ,
PRIMARY KEY(garcom_id, dispositivo_acao_login_id)      ,
  FOREIGN KEY(dispositivo_acao_login_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(garcom_id)
    REFERENCES garcom(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(dispositivo_acao_logoff_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX garcom_logado_FKIndex1 ON garcom_logado (dispositivo_acao_login_id);
CREATE INDEX garcom_logado_FKIndex2 ON garcom_logado (garcom_id);
CREATE INDEX garcom_logado_FKIndex3 ON garcom_logado (dispositivo_acao_logoff_id);



CREATE TABLE dispositivo_loja_servidor (
  dispositivo_id BIGINT  NOT NULL  ,
  loja_id INTEGER  NOT NULL  ,
  servidor_id INTEGER  NOT NULL    ,
PRIMARY KEY(dispositivo_id, loja_id, servidor_id)      ,
  FOREIGN KEY(dispositivo_id)
    REFERENCES dispositivo(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(servidor_id)
    REFERENCES servidor(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX dls_FKIndex1 ON dispositivo_loja_servidor (dispositivo_id);
CREATE INDEX dls_FKIndex2 ON dispositivo_loja_servidor (loja_id);
CREATE INDEX dls_FKIndex3 ON dispositivo_loja_servidor (servidor_id);



CREATE TABLE produto (
  id INTEGER  NOT NULL  ,
  unidade_medida_id INTEGER    ,
  grupo_produto_id INTEGER  NOT NULL  ,
  arquivo_disponivel_id BIGINT    ,
  loja_id INTEGER  NOT NULL  ,
  descricao dm_descricao  NOT NULL  ,
  preco_venda dm_money  NOT NULL  ,
  app_status dm_bool  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER      ,  
  QUANT_INGR_CORTESIA integer,
PRIMARY KEY(id)        ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(arquivo_disponivel_id)
    REFERENCES arquivo_disponivel(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(grupo_produto_id)
    REFERENCES grupo_produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(unidade_medida_id)
    REFERENCES unidade_medida(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX produto_FKIndex1 ON produto (loja_id);
CREATE INDEX produto_FKIndex2 ON produto (arquivo_disponivel_id);
CREATE INDEX produto_FKIndex3 ON produto (grupo_produto_id);
CREATE INDEX produto_FKIndex4 ON produto (unidade_medida_id);



CREATE TABLE usuario (
  id INTEGER  NOT NULL  ,
  dispositivo_acao_cadastro_id BIGINT  NOT NULL  ,
  cpf dm_cnpj  NOT NULL  ,
  nome dm_nome  NOT NULL  ,
  sobrenome dm_sobrenome  NOT NULL  ,
  email DM_EMAIL  NOT NULL  ,
  senha VARCHAR(100)  NOT NULL  ,
  tipo_usuario dm_tipo_usuario  NOT NULL  ,
  facebook_id VARCHAR(35)      ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(dispositivo_acao_cadastro_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE UNIQUE INDEX usuario_unique ON usuario (cpf, email, facebook_id);
CREATE INDEX usuario_FKIndex1 ON usuario (dispositivo_acao_cadastro_id);

COMMENT ON COLUMN usuario.facebook_id IS 'Quando o usuário efetua login/cadastro pelo facebook';


CREATE TABLE usuario_bonus (
  usuario_id INTEGER NOT NULL,
  QUANT_BONUS dm_money  NOT NULL,
PRIMARY KEY(usuario_id)  ,
  FOREIGN KEY(usuario_id)
    REFERENCES usuario(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);

CREATE INDEX usuario_bonus_FKIndex1 ON usuario_bonus (usuario_id);


CREATE TABLE lista_negra (
  usuario_id INTEGER  NOT NULL    ,
  FOREIGN KEY(usuario_id)
    REFERENCES usuario(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX lista_negra_FKIndex1 ON lista_negra (usuario_id);



CREATE TABLE dispositivo_app_token_fcm (
  dispositivo_acao_id BIGINT  NOT NULL  ,
  app_token_fcm VARCHAR(300)  NOT NULL    ,
PRIMARY KEY(dispositivo_acao_id)  ,
  FOREIGN KEY(dispositivo_acao_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX dispositivo_at_FKIndex1 ON dispositivo_app_token_fcm (dispositivo_acao_id);



CREATE TABLE dispositivo_app_versao (
  dispositivo_acao_id BIGINT  NOT NULL  ,
  app_versao INTEGER  NOT NULL    ,
PRIMARY KEY(dispositivo_acao_id)  ,
  FOREIGN KEY(dispositivo_acao_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX dispositivo_av_FKIndex1 ON dispositivo_app_versao (dispositivo_acao_id);



CREATE TABLE cidade (
  id INTEGER  NOT NULL  ,
  estado_id INTEGER  NOT NULL  ,
  nome dm_nome  NOT NULL  ,
  codigo_ibge CHAR(7)      ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(estado_id)
    REFERENCES estado(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE UNIQUE INDEX cidade_index1033 ON cidade (codigo_ibge);
CREATE INDEX cidade_FKIndex1 ON cidade (estado_id);



-- ------------------------------------------------------------
-- Em casos de requisiçes FCM serve para registra qual token foi usado
-- ------------------------------------------------------------

CREATE TABLE uso_api_externa_token_fcm (
  uso_api_externa_id BIGINT  NOT NULL  ,
  dispositivo_app_token_id BIGINT  NOT NULL    ,
PRIMARY KEY(uso_api_externa_id, dispositivo_app_token_id)    ,
  FOREIGN KEY(uso_api_externa_id)
    REFERENCES uso_api_externa(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(dispositivo_app_token_id)
    REFERENCES dispositivo_app_token_fcm(dispositivo_acao_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX uso_a_fcm_FKIndex1 ON uso_api_externa_token_fcm (uso_api_externa_id);
CREATE INDEX uso_a_fcm_FKIndex2 ON uso_api_externa_token_fcm (dispositivo_app_token_id);

COMMENT ON TABLE uso_api_externa_token_fcm IS 'Em casos de requisiçes FCM serve para registra qual token foi usado';


CREATE TABLE usuarios_bloqueados (
  loja_id INTEGER  NOT NULL  ,
  usuario_id INTEGER  NOT NULL    ,
PRIMARY KEY(loja_id, usuario_id)    ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(usuario_id)
    REFERENCES usuario(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX ub_FKIndex1 ON usuarios_bloqueados (loja_id);
CREATE INDEX ub_FKIndex2 ON usuarios_bloqueados (usuario_id);



CREATE TABLE usuario_telefone (
  id BIGINT  NOT NULL  ,
  usuario_id INTEGER  NOT NULL  ,
  telefone_id BIGINT  NOT NULL    ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(telefone_id)
    REFERENCES telefone(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(usuario_id)
    REFERENCES usuario(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX usuario_t_FKIndex1 ON usuario_telefone (telefone_id);
CREATE INDEX usuario_t_FKIndex2 ON usuario_telefone (usuario_id);



CREATE TABLE cidade_arquivo_disponivel (
  cidade_id INTEGER  NOT NULL  ,
  arquivo_disponivel_id BIGINT  NOT NULL    ,
PRIMARY KEY(cidade_id, arquivo_disponivel_id)    ,
  FOREIGN KEY(cidade_id)
    REFERENCES cidade(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(arquivo_disponivel_id)
    REFERENCES arquivo_disponivel(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX cidade_disponivel_FKIndex1 ON cidade_arquivo_disponivel (cidade_id);
CREATE INDEX cidade_disponivel_FKIndex2 ON cidade_arquivo_disponivel (arquivo_disponivel_id);



CREATE TABLE sabor_produto_disponivel (
  sabor_id INTEGER  NOT NULL  ,
  produto_id INTEGER  NOT NULL  ,
  app_status dm_bool  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL    ,
PRIMARY KEY(sabor_id, produto_id)    ,
  FOREIGN KEY(sabor_id)
    REFERENCES sabor(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(produto_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX sabor_p_FKIndex1 ON sabor_produto_disponivel (sabor_id);
CREATE INDEX sabor_p_FKIndex2 ON sabor_produto_disponivel (produto_id);



CREATE TABLE sabor_produto_ingrediente (
  ingrediente_id INTEGER  NOT NULL  ,
  spd_produto_id INTEGER  NOT NULL  ,
  spd_sabor_id INTEGER  NOT NULL  ,
  valor_acrescimo dm_money    ,
  app_status dm_bool  NOT NULL  ,
  app_status_ingrediente SMALLINT    ,
  last_update TIMESTAMP  NOT NULL  ,
  base_contratante_id INTEGER      ,
PRIMARY KEY(ingrediente_id, spd_produto_id, spd_sabor_id)    ,
  FOREIGN KEY(spd_sabor_id, spd_produto_id)
    REFERENCES sabor_produto_disponivel(sabor_id, produto_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(ingrediente_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX spdp_FKIndex1 ON sabor_produto_ingrediente (spd_sabor_id, spd_produto_id);
CREATE INDEX spdp_FKIndex2 ON sabor_produto_ingrediente (ingrediente_id);



CREATE TABLE produto_ingrediente (
  produto_id INTEGER  NOT NULL  ,
  ingrediente_id INTEGER  NOT NULL  ,
  ingrediente_substituicao_id INTEGER    ,
  valor_acrescimo dm_money    ,
  app_status_ingrediente SMALLINT  NOT NULL  ,
  app_status dm_bool  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL    ,
PRIMARY KEY(produto_id, ingrediente_id)      ,
  FOREIGN KEY(produto_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(ingrediente_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(ingrediente_substituicao_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX produto_p_FKIndex1 ON produto_ingrediente (produto_id);
CREATE INDEX produto_p_FKIndex2 ON produto_ingrediente (ingrediente_id);
CREATE INDEX produto_p_FKIndex3 ON produto_ingrediente (ingrediente_substituicao_id);



CREATE TABLE pedido_cliente_item (
  id BIGINT  NOT NULL  ,
  pedido_cliente_id BIGINT  NOT NULL  ,
  tamanho_produto_id INTEGER    ,
  produto_id INTEGER  NOT NULL  ,
  quantidade INTEGER  NOT NULL  ,
  valor_unidade_cobrado dm_money  NOT NULL  ,
  observacoes DM_TEXTO      ,
PRIMARY KEY(id)      ,
  FOREIGN KEY(pedido_cliente_id)
    REFERENCES pedido_cliente(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(produto_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(tamanho_produto_id)
    REFERENCES tamanho_produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX peci_FKIndex1 ON pedido_cliente_item (pedido_cliente_id);
CREATE INDEX peci_FKIndex2 ON pedido_cliente_item (produto_id);
CREATE INDEX peci_FKIndex3 ON pedido_cliente_item (tamanho_produto_id);



CREATE TABLE usuario_logado (
  usuario_id INTEGER  NOT NULL  ,
  dispositivo_acao_login_id BIGINT  NOT NULL  ,
  dispositivo_acao_logoff_id BIGINT      ,
PRIMARY KEY(usuario_id, dispositivo_acao_login_id)      ,
  FOREIGN KEY(usuario_id)
    REFERENCES usuario(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(dispositivo_acao_logoff_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(dispositivo_acao_login_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX dispositivo_acao_u_FKIndex2 ON usuario_logado (usuario_id);
CREATE INDEX usuario_logado_FKIndex3 ON usuario_logado (dispositivo_acao_logoff_id);
CREATE INDEX usuario_logado_FKIndex5 ON usuario_logado (dispositivo_acao_login_id);

COMMENT ON COLUMN usuario_logado.dispositivo_acao_login_id IS 'Dispositivo logado';
COMMENT ON COLUMN usuario_logado.dispositivo_acao_logoff_id IS 'Se o id do dispositivo dessa acao for diferente do login representa um logoff forçado por outro dispositivo';


CREATE TABLE opcional_produto_disponivel (
  opcional_produto_id INTEGER  NOT NULL  ,
  produto_id INTEGER  NOT NULL  ,
  arquivo_disponivel_id BIGINT    ,
  app_status dm_bool  NOT NULL  ,
  app_tela SMALLINT  NOT NULL  ,
  app_is_default dm_bool  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL    ,
PRIMARY KEY(opcional_produto_id, produto_id)      ,
  FOREIGN KEY(opcional_produto_id)
    REFERENCES opcional_produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(produto_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(arquivo_disponivel_id)
    REFERENCES arquivo_disponivel(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX opp_FKIndex1 ON opcional_produto_disponivel (opcional_produto_id);
CREATE INDEX opp_FKIndex2 ON opcional_produto_disponivel (produto_id);
CREATE INDEX opp_FKIndex3 ON opcional_produto_disponivel (arquivo_disponivel_id);



CREATE TABLE tamanho_produto_disponivel (
  tamanho_produto_id INTEGER  NOT NULL  ,
  produto_id INTEGER  NOT NULL  ,
  arquivo_disponivel_id BIGINT    ,
  preco_venda dm_money    ,
  app_status dm_bool  NOT NULL  ,
  limite_sabores SMALLINT    ,
  limite_opcionais SMALLINT    ,
  numero_fatias INTEGER    ,
  tamanho_massa DECIMAL(3,2)    ,
  last_update TIMESTAMP  NOT NULL    ,
PRIMARY KEY(tamanho_produto_id, produto_id)      ,
  FOREIGN KEY(tamanho_produto_id)
    REFERENCES tamanho_produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(produto_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(arquivo_disponivel_id)
    REFERENCES arquivo_disponivel(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX tamanho_produto_d_FKIndex1 ON tamanho_produto_disponivel (tamanho_produto_id);
CREATE INDEX tamanho_produto_dFKIndex2 ON tamanho_produto_disponivel (produto_id);
CREATE INDEX tamanho_produto_d_FKIndex3 ON tamanho_produto_disponivel (arquivo_disponivel_id);



CREATE TABLE bairro (
  id INTEGER  NOT NULL  ,
  cidade_id INTEGER  NOT NULL  ,
  nome dm_nome  NOT NULL    ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(cidade_id)
    REFERENCES cidade(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX bairro_FKIndex1 ON bairro (cidade_id);



CREATE TABLE endereco (
  id INTEGER  NOT NULL  ,
  bairro_id INTEGER  NOT NULL  ,
  logradouro dm_descricao  NOT NULL  ,
  numero VARCHAR(10)  NOT NULL  ,
  complemento dm_descricao_longa    ,
  referencia dm_referencia    ,
  latitude DECIMAL(11,8)    ,
  longitude DECIMAL(11,8)    ,
  cep dm_cep      ,
PRIMARY KEY(id)  ,
  FOREIGN KEY(bairro_id)
    REFERENCES bairro(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX endereco_FKIndex1 ON endereco (bairro_id);



CREATE TABLE pedido_cliente_item_ingr (
  pedido_cliente_item_id BIGINT  NOT NULL  ,
  pi_ingrediente_id INTEGER  NOT NULL  ,
  pi_produto_id INTEGER  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL  ,
  is_removido dm_bool  NOT NULL    ,
PRIMARY KEY(pedido_cliente_item_id, pi_ingrediente_id, pi_produto_id)    ,
  FOREIGN KEY(pedido_cliente_item_id)
    REFERENCES pedido_cliente_item(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(pi_produto_id, pi_ingrediente_id)
    REFERENCES produto_ingrediente(produto_id, ingrediente_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX pcei_ingr_FKIndex1 ON pedido_cliente_item_ingr (pedido_cliente_item_id);
CREATE INDEX pcei_ingr_FKIndex2 ON pedido_cliente_item_ingr (pi_produto_id, pi_ingrediente_id);



CREATE TABLE pedido_cliente_item_sabor (
  id INTEGER  NOT NULL  ,
  sabor_id INTEGER  NOT NULL  ,
  pedido_cliente_item_id BIGINT  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL    ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(pedido_cliente_item_id)
    REFERENCES pedido_cliente_item(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(sabor_id)
    REFERENCES sabor(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX peci_sab_FKIndex1 ON pedido_cliente_item_sabor (pedido_cliente_item_id);
CREATE INDEX peci_sab_FKIndex2 ON pedido_cliente_item_sabor (sabor_id);



CREATE TABLE pedido_cliente_item_opc (
  pedido_cliente_item_id BIGINT  NOT NULL  ,
  opcional_produto_id INTEGER  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL    ,
PRIMARY KEY(pedido_cliente_item_id, opcional_produto_id)    ,
  FOREIGN KEY(pedido_cliente_item_id)
    REFERENCES pedido_cliente_item(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(opcional_produto_id)
    REFERENCES opcional_produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX peci_op_FKIndex1 ON pedido_cliente_item_opc (pedido_cliente_item_id);
CREATE INDEX peci_op_FKIndex2 ON pedido_cliente_item_opc (opcional_produto_id);



CREATE TABLE pedido_cliente_item_sab_ingre (
  pcis_sabor_id INTEGER  NOT NULL  ,
  spd_sabor_id INTEGER  NOT NULL  ,
  spd_produto_id INTEGER  NOT NULL  ,
  spi_ingrediente_id INTEGER  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL  ,
  is_removido dm_bool  NOT NULL    ,
PRIMARY KEY(pcis_sabor_id, spd_sabor_id, spd_produto_id, spi_ingrediente_id)    ,
  FOREIGN KEY(pcis_sabor_id)
    REFERENCES pedido_cliente_item_sabor(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(spi_ingrediente_id, spd_produto_id, spd_sabor_id)
    REFERENCES sabor_produto_ingrediente(ingrediente_id, spd_produto_id, spd_sabor_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX peci_sab_ingr_FKIndex1 ON pedido_cliente_item_sab_ingre (pcis_sabor_id);
CREATE INDEX peci_sab_ingr_FKIndex2 ON pedido_cliente_item_sab_ingre (spi_ingrediente_id, spd_produto_id, spd_sabor_id);



CREATE TABLE tpd_spd_valor (
  produto_id INTEGER  NOT NULL  ,
  tamanho_produto_id INTEGER  NOT NULL  ,
  sabor_id INTEGER  NOT NULL  ,
  valor_sabor dm_money  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL    ,
PRIMARY KEY(produto_id, tamanho_produto_id, sabor_id)    ,
  FOREIGN KEY(tamanho_produto_id, produto_id)
    REFERENCES tamanho_produto_disponivel(tamanho_produto_id, produto_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(sabor_id, produto_id)
    REFERENCES sabor_produto_disponivel(sabor_id, produto_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX tpd_spd_valor_FKIndex1 ON tpd_spd_valor (tamanho_produto_id, produto_id);
CREATE INDEX tpd_spd_valor_FKIndex2 ON tpd_spd_valor (sabor_id, produto_id);



CREATE TABLE usuario_endereco (
  endereco_id INTEGER  NOT NULL  ,
  usuario_id INTEGER  NOT NULL  ,
  app_status dm_bool  NOT NULL DEFAULT 1   ,
PRIMARY KEY(endereco_id, usuario_id)    ,
  FOREIGN KEY(endereco_id)
    REFERENCES endereco(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(usuario_id)
    REFERENCES usuario(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX endereco_has_usuario_FKIndex1 ON usuario_endereco (endereco_id);
CREATE INDEX endereco_has_usuario_FKIndex2 ON usuario_endereco (usuario_id);



CREATE TABLE cliente_endereco (
  cliente_id INTEGER  NOT NULL  ,
  endereco_id INTEGER  NOT NULL    ,
PRIMARY KEY(cliente_id, endereco_id)    ,
  FOREIGN KEY(cliente_id)
    REFERENCES cliente(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(endereco_id)
    REFERENCES endereco(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX cliente_he_FKIndex1 ON cliente_endereco (cliente_id);
CREATE INDEX cliente_he_FKIndex2 ON cliente_endereco (endereco_id);



CREATE TABLE distancia_endereco (
  id INTEGER  NOT NULL  ,
  destino_endereco_id INTEGER  NOT NULL  ,
  origem_endereco_id INTEGER  NOT NULL  ,
  distancia DECIMAL(3,1)    ,
  tempo TIME      ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(origem_endereco_id)
    REFERENCES endereco(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(destino_endereco_id)
    REFERENCES endereco(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX distancia_endereco_FKIndex1 ON distancia_endereco (origem_endereco_id);
CREATE INDEX distancia_endereco_FKIndex2 ON distancia_endereco (destino_endereco_id);



CREATE TABLE mesa_fechamento_parcial_item (
  id INTEGER  NOT NULL  ,
  pedido_cliente_item_id BIGINT  NOT NULL  ,
  mesa_fechamento_id INTEGER  NOT NULL  ,
  quantidade INTEGER  NOT NULL    ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(mesa_fechamento_id)
    REFERENCES mesa_fechamento(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(pedido_cliente_item_id)
    REFERENCES pedido_cliente_item(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX mfpi_FKIndex1 ON mesa_fechamento_parcial_item (mesa_fechamento_id);
CREATE INDEX mfpi_FKIndex2 ON mesa_fechamento_parcial_item (pedido_cliente_item_id);



CREATE TABLE loja_endereco (
  loja_id INTEGER  NOT NULL  ,
  endereco_id INTEGER  NOT NULL  ,
  status_endereco dm_uso_status      ,
PRIMARY KEY(loja_id, endereco_id)    ,
  FOREIGN KEY(loja_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(endereco_id)
    REFERENCES endereco(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX loja_has_endereco_FKIndex1 ON loja_endereco (loja_id);
CREATE INDEX loja_has_endereco_FKIndex2 ON loja_endereco (endereco_id);

COMMENT ON COLUMN loja_endereco.status_endereco IS '1 => Atual | 0 => Sem Uso';


CREATE TABLE pedido_cliente_agenda (
  pedido_cliente_id BIGINT  NOT NULL  ,
  cliente_endereco_endereco_id INTEGER  NOT NULL  ,
  cliente_endereco_cliente_id INTEGER  NOT NULL    ,
PRIMARY KEY(pedido_cliente_id)    ,
  FOREIGN KEY(pedido_cliente_id)
    REFERENCES pedido_cliente(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(cliente_endereco_cliente_id, cliente_endereco_endereco_id)
    REFERENCES cliente_endereco(cliente_id, endereco_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX pedido_cliente_ce_FKIndex1 ON pedido_cliente_agenda (pedido_cliente_id);
CREATE INDEX pedido_cliente_ce_FKIndex2 ON pedido_cliente_agenda (cliente_endereco_cliente_id, cliente_endereco_endereco_id);



CREATE TABLE op_prod_valor_tamanho_prod (
  produto_id INTEGER  NOT NULL  ,
  tpd_tamanho_id INTEGER  NOT NULL  ,
  opd_opcional_id INTEGER  NOT NULL  ,
  valor_opcional dm_money  NOT NULL  ,
  last_update TIMESTAMP  NOT NULL    ,
PRIMARY KEY(produto_id, tpd_tamanho_id, opd_opcional_id)    ,
  FOREIGN KEY(opd_opcional_id, produto_id)
    REFERENCES opcional_produto_disponivel(opcional_produto_id, produto_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(tpd_tamanho_id, produto_id)
    REFERENCES tamanho_produto_disponivel(tamanho_produto_id, produto_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX tpdopd_FKIndex1 ON op_prod_valor_tamanho_prod (opd_opcional_id, produto_id);
CREATE INDEX tpdopd_FKIndex2 ON op_prod_valor_tamanho_prod (tpd_tamanho_id, produto_id);



CREATE TABLE pedido (
  id BIGINT  NOT NULL  ,
  forma_entrega_id INTEGER    ,
  forma_pagamento_id INTEGER    ,
  usuario_telefone_id BIGINT    ,
  loja_bonus_conf_id INTEGER  NOT NULL  ,
  loja_endereco_endereco_id INTEGER  NOT NULL  ,
  loja_endereco_loja_id INTEGER  NOT NULL  ,
  usuario_endereco_usuario_id INTEGER  NOT NULL  ,
  usuario_endereco_endereco_id INTEGER  NOT NULL  ,
  dispositivo_acao_id BIGINT  NOT NULL  ,
  monitor_sc_coletor_acao_id BIGINT    ,
  monitor_sc_falha_acao_id BIGINT    ,
  monitor_sc_cancelador_acao_id BIGINT    ,
  monitor_sc_confirmador_acao_id BIGINT    ,
  total_tmp dm_money    ,
  json_tmp_sc VARCHAR(5000)    ,
  pago_em_bonus dm_bool  NOT NULL  ,
  pedido_id_antigo_capp INTEGER    ,
  is_cancelado dm_bool  NOT NULL DEFAULT 0 ,
  horario_estimado_entrega TIMESTAMP    ,
  valor_entrega_cobrado dm_money    ,
  valor_pagamento_cobrado dm_money    ,
  valor_acrescimo_cobrado dm_money    ,
  valor_troco dm_money    ,
  distancia_loja_entrega dm_money    ,
  is_novo dm_bool  NOT NULL DEFAULT 0   ,
PRIMARY KEY(id)                          ,
  FOREIGN KEY(usuario_endereco_endereco_id, usuario_endereco_usuario_id)
    REFERENCES usuario_endereco(endereco_id, usuario_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(loja_endereco_loja_id, loja_endereco_endereco_id)
    REFERENCES loja_endereco(loja_id, endereco_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(loja_bonus_conf_id)
    REFERENCES loja_bonus_conf(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(dispositivo_acao_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(monitor_sc_coletor_acao_id)
    REFERENCES monitor_sc_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(monitor_sc_confirmador_acao_id)
    REFERENCES monitor_sc_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(usuario_telefone_id)
    REFERENCES usuario_telefone(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(monitor_sc_falha_acao_id)
    REFERENCES monitor_sc_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(monitor_sc_cancelador_acao_id)
    REFERENCES monitor_sc_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(forma_pagamento_id)
    REFERENCES forma_pagamento(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(forma_pagamento_id)
    REFERENCES forma_pagamento(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(forma_entrega_id)
    REFERENCES forma_entrega(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX pedido_FKIndex1 ON pedido (usuario_endereco_endereco_id, usuario_endereco_usuario_id);
CREATE INDEX pedido_FKIndex2 ON pedido (loja_endereco_loja_id, loja_endereco_endereco_id);
CREATE INDEX pedido_FKIndex3 ON pedido (loja_bonus_conf_id);
CREATE INDEX pedido_FKIndex4 ON pedido (dispositivo_acao_id);
CREATE INDEX pedido_FKIndex5 ON pedido (monitor_sc_coletor_acao_id);
CREATE INDEX pedido_FKIndex6 ON pedido (monitor_sc_confirmador_acao_id);
CREATE INDEX pedido_FKIndex7 ON pedido (usuario_telefone_id);
CREATE UNIQUE INDEX pedido_unique ON pedido (pedido_id_antigo_capp);
CREATE INDEX pedido_FKIndex8 ON pedido (monitor_sc_falha_acao_id);
CREATE INDEX pedido_FKIndex9 ON pedido (monitor_sc_cancelador_acao_id);
CREATE INDEX pedido_FKIndex10 ON pedido (forma_pagamento_id);
CREATE INDEX pedido_FKIndex11 ON pedido (forma_pagamento_id);
CREATE INDEX pedido_FKIndex12 ON pedido (forma_entrega_id);

COMMENT ON COLUMN pedido.monitor_sc_cancelador_acao_id IS '!null => Pedido foi cancelo pelo mg ou o mg coletou o cancelamento disparado pelo adm';
COMMENT ON COLUMN pedido.monitor_sc_confirmador_acao_id IS 'null => Em espera | !null => Pedido confirmado e bonus liberado';
COMMENT ON COLUMN pedido.total_tmp IS 'Temporario';
COMMENT ON COLUMN pedido.json_tmp_sc IS 'Temporario';
COMMENT ON COLUMN pedido.pago_em_bonus IS '0 => Nao | 1 => Sim';
COMMENT ON COLUMN pedido.pedido_id_antigo_capp IS 'Id no antigo capp. Usado em pedidos importados de la';
COMMENT ON COLUMN pedido.is_cancelado IS '0 => Nao | 1 => Sim';
COMMENT ON COLUMN pedido.distancia_loja_entrega IS 'Distância em Km entre a loja e o endereço de entrega';
COMMENT ON COLUMN pedido.is_novo IS 'Indica se o pedido é feito a partir do app refatorado via REST';


CREATE TABLE notificacao (
  id BIGINT  NOT NULL  ,
  pedido_id BIGINT    ,
  loja_responsavel_id INTEGER    ,
  horario_preparo TIMESTAMP  NOT NULL  ,
  titulo VARCHAR(150)    ,
  conteudo DM_TEXTO  NOT NULL  ,
  tipo dm_notificacao_tipo  NOT NULL    ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(loja_responsavel_id)
    REFERENCES loja(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(pedido_id)
    REFERENCES pedido(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX notificacao_FKIndex1 ON notificacao (loja_responsavel_id);
CREATE INDEX notificacao_FKIndex2 ON notificacao (pedido_id);

COMMENT ON COLUMN notificacao.pedido_id IS 'Se possuí alguma relaçao com o pedido';
COMMENT ON COLUMN notificacao.loja_responsavel_id IS 'Null => Central';


CREATE TABLE pedido_item (
  id BIGINT  NOT NULL  ,
  tamanho_produto_id INTEGER    ,
  produto_id INTEGER  NOT NULL  ,
  pedido_id BIGINT  NOT NULL  ,
  quantidade INTEGER  NOT NULL  ,
  valor_unidade_cobrado dm_money  NOT NULL    ,
PRIMARY KEY(id)      ,
  FOREIGN KEY(pedido_id)
    REFERENCES pedido(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(produto_id)
    REFERENCES produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(tamanho_produto_id)
    REFERENCES tamanho_produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX pedido_item_FKIndex1 ON pedido_item (pedido_id);
CREATE INDEX pedido_item_FKIndex2 ON pedido_item (produto_id);
CREATE INDEX pedido_item_FKIndex3 ON pedido_item (tamanho_produto_id);


CREATE TABLE pedido_observacao (
  id BIGINT  NOT NULL  ,
  pedido_id BIGINT  NOT NULL  ,
  observacao DM_TEXTO  NOT NULL  ,
PRIMARY KEY(id)      ,
  FOREIGN KEY(pedido_id)
    REFERENCES pedido(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX pedido_observacao_FKIndex1 ON pedido_observacao (pedido_id);



CREATE TABLE notificacao_dispositivo (
  notificacao_id BIGINT  NOT NULL  ,
  dispositivo_id BIGINT  NOT NULL  ,
  d_a_recebimento_id BIGINT    ,
  d_a_visualizacao_id BIGINT      ,
PRIMARY KEY(notificacao_id, dispositivo_id)        ,
  FOREIGN KEY(notificacao_id)
    REFERENCES notificacao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(dispositivo_id)
    REFERENCES dispositivo(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(d_a_visualizacao_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(d_a_recebimento_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX notificacao_d_FKIndex1 ON notificacao_dispositivo (notificacao_id);
CREATE INDEX notificacao_d_FKIndex2 ON notificacao_dispositivo (dispositivo_id);
CREATE INDEX notificacao_d_FKIndex3 ON notificacao_dispositivo (d_a_visualizacao_id);
CREATE INDEX notificacao_d_FKIndex4 ON notificacao_dispositivo (d_a_recebimento_id);



CREATE TABLE notificacao_usuario (
  notificacao_id BIGINT  NOT NULL  ,
  usuario_id INTEGER  NOT NULL  ,
  d_a_recebimento_id BIGINT    ,
  d_a_visualizacao_id BIGINT      ,
PRIMARY KEY(notificacao_id, usuario_id)        ,
  FOREIGN KEY(notificacao_id)
    REFERENCES notificacao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(usuario_id)
    REFERENCES usuario(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(d_a_recebimento_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(d_a_visualizacao_id)
    REFERENCES dispositivo_acao(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX notificacao_u_FKIndex1 ON notificacao_usuario (notificacao_id);
CREATE INDEX notificacao_u_FKIndex2 ON notificacao_usuario (usuario_id);
CREATE INDEX notificacao_u_FKIndex3 ON notificacao_usuario (d_a_recebimento_id);
CREATE INDEX notificacao_u_FKIndex4 ON notificacao_usuario (d_a_visualizacao_id);



CREATE TABLE pedido_item_opcional_prod (
  pedido_item_id BIGINT  NOT NULL  ,
  opcional_produto_id INTEGER  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL    ,
PRIMARY KEY(pedido_item_id, opcional_produto_id)    ,
  FOREIGN KEY(pedido_item_id)
    REFERENCES pedido_item(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(opcional_produto_id)
    REFERENCES opcional_produto(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX opcional_ppi_FKIndex1 ON pedido_item_opcional_prod (pedido_item_id);
CREATE INDEX opcional_ppi_FKIndex2 ON pedido_item_opcional_prod (opcional_produto_id);



CREATE TABLE pedido_item_ingr (
  pedido_item_id BIGINT  NOT NULL  ,
  pi_ingrediente_id INTEGER  NOT NULL  ,
  pi_produto_id INTEGER  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL  ,
  is_removido dm_bool  NOT NULL    ,
PRIMARY KEY(pedido_item_id, pi_ingrediente_id, pi_produto_id)    ,
  FOREIGN KEY(pedido_item_id)
    REFERENCES pedido_item(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(pi_produto_id, pi_ingrediente_id)
    REFERENCES produto_ingrediente(produto_id, ingrediente_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX prod_ingre_pi_FKIndex1 ON pedido_item_ingr (pedido_item_id);
CREATE INDEX prod_ingre_pi_FKIndex2 ON pedido_item_ingr (pi_produto_id, pi_ingrediente_id);



CREATE TABLE pedido_item_sabor_prod (
  pedido_item_id BIGINT  NOT NULL  ,
  sabor_id INTEGER  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL    ,
PRIMARY KEY(pedido_item_id, sabor_id)    ,
  FOREIGN KEY(pedido_item_id)
    REFERENCES pedido_item(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(sabor_id)
    REFERENCES sabor(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX sabor_ppi_FKIndex1 ON pedido_item_sabor_prod (pedido_item_id);
CREATE INDEX sabor_ppi_FKIndex2 ON pedido_item_sabor_prod (sabor_id);



CREATE TABLE pedido_item_sabor_prod2 (
  id INTEGER  NOT NULL  ,
  sabor_id INTEGER  NOT NULL  ,
  pedido_item_id BIGINT  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL    ,
PRIMARY KEY(id)    ,
  FOREIGN KEY(pedido_item_id)
    REFERENCES pedido_item(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(sabor_id)
    REFERENCES sabor(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX pedido_isp2_FKIndex1 ON pedido_item_sabor_prod2 (pedido_item_id);
CREATE INDEX pedido_isp2_FKIndex2 ON pedido_item_sabor_prod2 (sabor_id);



CREATE TABLE pedido_item_sabor_ingr (
  sbi_ingrediente_id INTEGER  NOT NULL  ,
  pis_sabor_id INTEGER  NOT NULL  ,
  pis_pedido_item_id BIGINT  NOT NULL  ,
  spd_sabor_id INTEGER  NOT NULL  ,
  spd_produto_id INTEGER  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL  ,
  is_removido dm_bool  NOT NULL    ,
PRIMARY KEY(sbi_ingrediente_id, pis_sabor_id, pis_pedido_item_id, spd_sabor_id, spd_produto_id)    ,
  FOREIGN KEY(pis_pedido_item_id, pis_sabor_id)
    REFERENCES pedido_item_sabor_prod(pedido_item_id, sabor_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(sbi_ingrediente_id, spd_produto_id, spd_sabor_id)
    REFERENCES sabor_produto_ingrediente(ingrediente_id, spd_produto_id, spd_sabor_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX sp_ipisp_FKIndex2 ON pedido_item_sabor_ingr (pis_pedido_item_id, pis_sabor_id);
CREATE INDEX sp_ipisp_FKIndex1 ON pedido_item_sabor_ingr (sbi_ingrediente_id, spd_produto_id, spd_sabor_id);



CREATE TABLE pedido_item_sabor_ingr2 (
  sbi_ingrediente_id INTEGER  NOT NULL  ,
  spd_sabor_id INTEGER  NOT NULL  ,
  spd_produto_id INTEGER  NOT NULL  ,
  PIS_PEDIDO_SABOR_PROD_ID INTEGER  NOT NULL  ,
  valor_cobrado dm_money  NOT NULL  ,
  is_removido dm_bool  NOT NULL    ,
PRIMARY KEY(sbi_ingrediente_id, spd_sabor_id, spd_produto_id, PIS_PEDIDO_SABOR_PROD_ID)    ,
  FOREIGN KEY(sbi_ingrediente_id, spd_produto_id, spd_sabor_id)
    REFERENCES sabor_produto_ingrediente(ingrediente_id, spd_produto_id, spd_sabor_id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION,
  FOREIGN KEY(PIS_PEDIDO_SABOR_PROD_ID)
    REFERENCES pedido_item_sabor_prod2(id)
      ON DELETE NO ACTION
      ON UPDATE NO ACTION);


CREATE INDEX spihpeiprod2_FKIndex1 ON pedido_item_sabor_ingr2 (sbi_ingrediente_id, spd_produto_id, spd_sabor_id);
CREATE INDEX spihpeiprod2_FKIndex2 ON pedido_item_sabor_ingr2 (PIS_PEDIDO_SABOR_PROD_ID);