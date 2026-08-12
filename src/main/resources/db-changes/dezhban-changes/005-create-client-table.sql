--liquibase formatted sql
/* https://docs.liquibase.com/concepts/changelogs/sql-format.html */

--changeset m_kalantari:1 context:table-creation
CREATE TABLE DEZHBAN.CLIENT
(
    ID                        NUMBER   (10, 0)         NOT NULL,
    CREATED_AT                TIMESTAMP WITH TIME ZONE NOT NULL,
    MODIFIED_AT               TIMESTAMP WITH TIME ZONE         ,
    NOT_BEFORE                TIMESTAMP WITH TIME ZONE         ,

    CLIENT_ID                 VARCHAR2 (100 CHAR)      NOT NULL,
    ENABLED                   NUMBER   (1)             NOT NULL,
    PUBLIC_CLIENT             NUMBER   (1)             NOT NULL,
    SECRET_HASH               VARCHAR2 (255 CHAR),
    SECRET_EXPIRES_AT         TIMESTAMP WITH TIME ZONE         ,

    CLIENT_TYPE               VARCHAR2 (50 CHAR)       NOT NULL,

    AUTHENTICATION_METHODS    VARCHAR2 (255)           NOT NULL,
    AUTHORIZATION_GRANT_TYPES VARCHAR2 (255)           NOT NULL,

    REDIRECT_URIS             VARCHAR2 (1000)          NOT NULL,
    POST_LOGOUT_REDIRECT_URI  VARCHAR2 (1000)                  ,
    SCOPES                    VARCHAR2 (1000)          NOT NULL,

    CLIENT_SETTINGS           VARCHAR2(4000)           NOT NULL,
    TOKEN_SETTINGS            VARCHAR2(4000)           NOT NULL,


    CONSTRAINT CLIENT_PK PRIMARY KEY (ID),
    CONSTRAINT CLIENT_CLIENT_ID_UK UNIQUE (CLIENT_ID),
    CONSTRAINT CLIENT_CLIENT_TYPE_CHK CHECK (CLIENT_TYPE IN ('AP', 'RO')),
    CONSTRAINT CLIENT_CLIENT_SETTINGS_CHK CHECK (CLIENT_SETTINGS IS JSON),
    CONSTRAINT CLIENT_TOKEN_SETTINGS_CHK CHECK (TOKEN_SETTINGS IS JSON)
);
--rollback DROP TABLE DEZHBAN.CLIENT;

--changeset m_kalantari:2 context:grant-access
GRANT SELECT,INSERT,UPDATE,DELETE ON DEZHBAN.CLIENT TO APP_DEZHBAN;


--changeset m_kalantari:3 context:sequence-creation
CREATE SEQUENCE DEZHBAN.CLIENT_SEQ INCREMENT BY 1
    MAXVALUE 9999999999
    MINVALUE 1
    CACHE 20;
--rollback DROP SEQUENCE DEZHBAN.CLIENT_SEQ;

--changeset m_kalantari:4 context:grant-access
GRANT SELECT on DEZHBAN.CLIENT_SEQ TO APP_DEZHBAN;