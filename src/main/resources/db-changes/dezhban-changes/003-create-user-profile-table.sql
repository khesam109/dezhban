--liquibase formatted sql
/* https://docs.liquibase.com/concepts/changelogs/sql-format.html */

--changeset m_kalantari:1 context:table-creation
CREATE TABLE DEZHBAN.USER_PROFILE
(
    USER_ID                NUMBER   (10, 0)         NOT NULL,
    CREATED_AT             TIMESTAMP WITH TIME ZONE NOT NULL,
    MODIFIED_AT            TIMESTAMP WITH TIME ZONE         ,

    FIRST_NAME             VARCHAR2 (255 CHAR)      NOT NULL,
    LAST_NAME              VARCHAR2 (255 CHAR)      NOT NULL,
    GENDER                 VARCHAR2 (6 CHAR)        NOT NULL,
    NATIONAL_CODE          VARCHAR2 (10 CHAR)       NOT NULL,
    ID_NUMBER              VARCHAR2 (10 CHAR)               ,
    BIRTH_DATE             VARCHAR2 (10 CHAR)       NOT NULL,
    FATHER_NAME            VARCHAR2 (255 CHAR)              ,
    LATIN_FIRST_NAME       VARCHAR2 (255 CHAR)              ,
    LATIN_LAST_NAME        VARCHAR2 (255 CHAR)              ,
    LATIN_FATHER_NAME      VARCHAR2 (255 CHAR)              ,

    NATIONALITY            VARCHAR2 (3 CHAR)        NOT NULL,
    POSTAL_CODE            VARCHAR2 (10 CHAR)               ,
    PROVINCE               VARCHAR2 (50 CHAR)               ,
    CITY                   VARCHAR2 (50 CHAR)               ,
    ADDRESS                VARCHAR2 (255 CHAR)              ,

    MOBILE_NUMBER          VARCHAR2 (14 CHAR)       NOT NULL,
    MOBILE_NUMBER_VERIFIED NUMBER   (1)                     ,
    PHONE_NUMBER           VARCHAR2 (14 CHAR)               ,
    EMAIL                  VARCHAR2 (100 CHAR)              ,
    EMAIL_VERIFIED         NUMBER   (1)                     ,


    CONSTRAINT USER_PROFILE_PK UNIQUE (USER_ID),
    CONSTRAINT USER_PROFILE_END_USER_FK FOREIGN KEY (USER_ID) REFERENCES END_USER(ID) ON DELETE CASCADE,
    CONSTRAINT USER_PROFILE_NATIONAL_CODE_UK UNIQUE (NATIONAL_CODE),
    CONSTRAINT USER_PROFILE_MOBILE_NUMBER_UK UNIQUE(MOBILE_NUMBER),
    CONSTRAINT USER_PROFILE_EMAIL_UK UNIQUE(EMAIL),
    CONSTRAINT USER_PROFILE_GENDER_CHK CHECK (GENDER IN ('MALE', 'FEMALE', 'OTHER'))
);
--rollback DROP TABLE DEZHBAN.USER_PROFILE;

--changeset m_kalantari:2 context:grant-access
GRANT SELECT,INSERT,UPDATE,DELETE ON DEZHBAN.USER_PROFILE TO APP_DEZHBAN;
