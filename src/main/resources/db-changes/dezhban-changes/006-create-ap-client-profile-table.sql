--liquibase formatted sql
/* https://docs.liquibase.com/concepts/changelogs/sql-format.html */

--changeset m_kalantari:1 context:table-creation
CREATE TABLE DEZHBAN.AP_CLIENT_PROFILE
(
    CLIENT_ID                 NUMBER   (10, 0)         NOT NULL,
    CREATED_AT                TIMESTAMP WITH TIME ZONE NOT NULL,
    MODIFIED_AT               TIMESTAMP WITH TIME ZONE         ,

    AP_TITLE                  VARCHAR2 (255)           NOT NULL,
    AP_CODE                   VARCHAR2 (255)           NOT NULL,
    AP_CALLBACK_URL           VARCHAR2 (255)           NOT NULL,
    COMMUNICATION_CERTIFICATE BLOB                             ,
    APPLICATION_CERTIFICATE   BLOB                             ,


    CONSTRAINT AP_CLIENT_PROFILE_CLIENT_FK FOREIGN KEY (CLIENT_ID) REFERENCES CLIENT(ID) ON DELETE CASCADE,
    CONSTRAINT AP_CLIENT_PROFILE_CLIENT_ID_UK UNIQUE (CLIENT_ID),
    CONSTRAINT AP_CLIENT_PROFILE_AP_CODE_UK UNIQUE (AP_CODE)
);
--rollback DROP TABLE DEZHBAN.AP_CLIENT_PROFILE;

--changeset m_kalantari:2 context:grant-access
GRANT SELECT,INSERT,UPDATE,DELETE ON DEZHBAN.AP_CLIENT_PROFILE TO APP_DEZHBAN;