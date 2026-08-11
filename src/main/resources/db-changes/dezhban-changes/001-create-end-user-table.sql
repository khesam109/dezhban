--liquibase formatted sql
/* https://docs.liquibase.com/concepts/changelogs/sql-format.html */

--changeset m_kalantari:1 context:table-creation
CREATE TABLE DEZHBAN.END_USER
(
    ID          NUMBER   (10, 0)           NOT NULL, -- internal id
    CREATED_AT  TIMESTAMP WITH TIME ZONE   NOT NULL,
    MODIFIED_AT TIMESTAMP WITH TIME ZONE           ,
    NOT_BEFORE  TIMESTAMP WITH TIME ZONE           ,

    SUBJECT     VARCHAR2 (36 CHAR)         NOT NULL, -- UUID, opaque identifier
    USERNAME    VARCHAR2 (255 CHAR)        NOT NULL,

    ENABLED     NUMBER   (1)     DEFAULT 1 NOT NULL,
    LOCKED      NUMBER   (1)     DEFAULT 0 NOT NULL,
    LOCK_UNTIL  TIMESTAMP WITH TIME ZONE           ,

    CONSTRAINT END_USER_PK PRIMARY KEY (ID),
    CONSTRAINT END_USER_SUBJECT_UK UNIQUE (SUBJECT),
    CONSTRAINT END_USER_USERNAME_UK UNIQUE (USERNAME)

);
--rollback DROP TABLE DEZHBAN.END_USER;

--changeset m_kalantari:2 context:grant-access
GRANT SELECT,INSERT,UPDATE,DELETE ON DEZHBAN.END_USER TO APP_DEZHBAN;


--changeset m_kalantari:3 context:sequence-creation
CREATE SEQUENCE DEZHBAN.END_USER_SEQ INCREMENT BY 1
    MAXVALUE 9999999999999999999999999999
    MINVALUE 1
    CACHE 20;
--rollback DROP SEQUENCE DEZHBAN.END_USER_SEQ;

--changeset m_kalantari:4 context:grant-access
GRANT SELECT on DEZHBAN.END_USER_SEQ TO APP_DEZHBAN;

-- --changeset m_kalantari:5 context:populate-data
-- INSERT INTO DEZHBAN.SYSTEM_USER VALUES
--     (
--         DEZHBAN.SYSTEM_USER_SEQ.nextval,
--      SYSDATE,
--      null,
--      null,
--      'hesam',
--      'salam',
--      'read'
--     )