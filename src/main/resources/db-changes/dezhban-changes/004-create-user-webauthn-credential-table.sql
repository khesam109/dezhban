--liquibase formatted sql
/* https://docs.liquibase.com/concepts/changelogs/sql-format.html */

--changeset m_kalantari:1 context:table-creation
CREATE TABLE DEZHBAN.USER_WEBAUTHN_CREDENTIAL
(
    ID                 NUMBER   (10, 0)         NOT NULL,
    USER_ID            NUMBER   (10, 0)         NOT NULL,
    CREATED_AT         TIMESTAMP WITH TIME ZONE NOT NULL,

    CREDENTIAL_ID      VARCHAR2 (500)           NOT NULL, -- The unique ID from the authenticator
    PUBLIC_KEY         VARCHAR2 (4000)          NOT NULL, -- The public key material
    SIGN_COUNT         NUMBER   (19) DEFAULT 0  NOT NULL,
    ATTESTATION_FORMAT VARCHAR2 (100)           NOT NULL,
    TRANSPORT          VARCHAR2 (255)                   , -- e.g., 'internal, usb, nfc' (helps Android client UI)

    IS_DEVICE_BOUND    NUMBER   (1) DEFAULT 0   NOT NULL, -- Hardware-bound flag (Relevant to your Android Keystore/sync requirements)

    LABEL              VARCHAR2 (100)                   , -- E.g., "My Pixel 8 Passkey"


    CONSTRAINT USER_WEBAUTHN_CREDENTIAL_PK PRIMARY KEY (ID),
    CONSTRAINT USER_WEBAUTHN_CREDENTIAL_END_USER_FK FOREIGN KEY (USER_ID) REFERENCES END_USER(ID) ON DELETE CASCADE
);
--rollback DROP TABLE DEZHBAN.USER_WEBAUTHN_CREDENTIAL;

--changeset m_kalantari:2 context:grant-access
GRANT SELECT,INSERT,UPDATE,DELETE ON DEZHBAN.USER_WEBAUTHN_CREDENTIAL TO APP_DEZHBAN;

--changeset m_kalantari:3 context:sequence-creation
CREATE SEQUENCE DEZHBAN.USER_WEBAUTHN_CREDENTIAL_SEQ INCREMENT BY 1
    MAXVALUE 9999999999999999999999999999
    MINVALUE 1
    CACHE 20;
--rollback DROP SEQUENCE DEZHBAN.USER_WEBAUTHN_CREDENTIAL_SEQ;

--changeset m_kalantari:4 context:grant-access
GRANT SELECT on DEZHBAN.USER_WEBAUTHN_CREDENTIAL_SEQ TO APP_DEZHBAN;