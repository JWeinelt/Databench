USE `datacat-store`;

CREATE TABLE IF NOT EXISTS accounts
(
    UserID             char(36)     NOT NULL,
    eMail              varchar(150) NOT NULL,
    Username           varchar(20)  NOT NULL,
    PasswordHashed     text         NOT NULL,
    Created            BIGINT       NOT NULL DEFAULT UNIX_TIMESTAMP(),
    LastLogin          BIGINT       NOT NULL DEFAULT UNIX_TIMESTAMP(),
    AccountStatus      char(36)     NOT NULL,
    VerificationStatus char(36)     NOT NULL,
    PRIMARY KEY (UserID, eMail, Username)
);

CREATE TABLE IF NOT EXISTS accounts_name_history
(
    UserID     char(36)    NOT NULL,
    ChangeDate BIGINT      NOT NULL DEFAULT UNIX_TIMESTAMP(),
    OldName    varchar(20) NOT NULL,
    NewName    varchar(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts_verification_statuses
(
    StatusID   char(36)    NOT NULL,
    StatusName varchar(20) NOT NULL,
    Color      char(15)    NOT NULL DEFAULT '000;000;000;100',
    PRIMARY KEY (StatusID, StatusName)
);
CREATE TABLE IF NOT EXISTS accounts_account_statuses
(
    StatusID   char(36)    NOT NULL,
    StatusName varchar(20) NOT NULL,
    Color      char(15)    NOT NULL DEFAULT '000;000;000;100'
);

CREATE TABLE IF NOT EXISTS yarn_status
(
    StatusID      char(36)    NOT NULL PRIMARY KEY,
    Name          varchar(20) NOT NULL,
    BgColor       char(15)    NOT NULL DEFAULT '000;000;000;100',
    FgColor       char(15)    NOT NULL DEFAULT '000;000;000;100',
    PublicVisible TINYINT     NOT NULL DEFAULT 0
);

INSERT INTO yarn_status (StatusID, Name, BgColor, FgColor, PublicVisible)
VALUES (UUID(), 'Draft', '107;114;128;100', '107;114;128;20', 0),
       (UUID(), 'Under Review', '201;151;000;100', '201;151;000;20', 0),
       (UUID(), 'Published', '046;125;050;100', '046;125;050;20', 0),
       (UUID(), 'Rejected', '176;064;064;100', '176;064;064;20', 0),
       (UUID(), 'Deleted', '090;090;090;100', '090;090;090;20', 0);

CREATE TABLE IF NOT EXISTS yarn_meta
(
    YarnID            char(36)       NOT NULL PRIMARY KEY,
    YarnName          varchar(50)    NOT NULL,
    AuthorID          char(36)       NOT NULL,
    Created           BIGINT         NOT NULL DEFAULT UNIX_TIMESTAMP(),
    LastUpdated       BIGINT         NOT NULL DEFAULT UNIX_TIMESTAMP(),
    Status            char(36)       NOT NULL,
    LongDescription   nvarchar(5000) NOT NULL,
    ShortDescription  nvarchar(100)  NOT NULL,
    FeatureHighlight1 varchar(30)    NOT NULL,
    FeatureHighlight2 varchar(30)    NOT NULL,
    FeatureHighlight3 varchar(30)    NOT NULL,
    WikiLink          varchar(100)   NOT NULL,
    DiscordLink       varchar(100)   NOT NULL,
    SourceLink        varchar(100)   NOT NULL,
    BannerID          char(36)       NULL,
    INDEX (AuthorID),
    INDEX (YarnName)
);

CREATE TABLE IF NOT EXISTS yarn_additional_links
(
    YarnID   char(36)     NOT NULL,
    LinkIcon varchar(50)  NOT NULL,
    LinkURL  varchar(100) NOT NULL,
    INDEX (YarnID, LinkIcon)
);

CREATE TABLE IF NOT EXISTS yarn_meta_tags_assigned
(
    YarnID          char(36) NOT NULL,
    TagID           char(36) NOT NULL,
    DisplayPriority INT      NOT NULL DEFAULT 0,
    INDEX (YarnID, TagID)
);
CREATE TABLE IF NOT EXISTS yarn_meta_tags
(
    TagID    char(36)    NOT NULL PRIMARY KEY,
    TagName  varchar(30) NOT NULL,
    TagColor char(15)    NOT NULL DEFAULT '000;000;000;100'
);

CREATE TABLE IF NOT EXISTS yarn_images
(
    YarnID       char(36)     NOT NULL,
    ImageID      char(36)     NOT NULL,
    GalleryOrder INT          NOT NULL DEFAULT 0,
    AltText      varchar(100) NOT NULL,
    INDEX (YarnID, ImageID)
);

CREATE TABLE IF NOT EXISTS yarn_live_data
(
    YarnID    char(36) NOT NULL,
    Date      BIGINT   NOT NULL DEFAULT UNIX_TIMESTAMP(),
    Downloads INT      NOT NULL DEFAULT 0,
    Views     INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (YarnID)
);

CREATE TABLE IF NOT EXISTS yarn_files
(
    YarnID       char(36) NOT NULL,
    FileID       char(36) NOT NULL,
    FileType     char(10) NOT NULL,
    FileSize     BIGINT   NOT NULL,
    CreationDate BIGINT   NOT NULL DEFAULT UNIX_TIMESTAMP(),
    INDEX (YarnID, FileID)
);

CREATE TABLE IF NOT EXISTS yarn_versions
(
    YarnID       CHAR(36)    NOT NULL,
    VersionID    CHAR(36)    NOT NULL,
    VersionName  VARCHAR(20) NOT NULL,
    CreationDate BIGINT      NOT NULL DEFAULT UNIX_TIMESTAMP(),
    INDEX (YarnID, VersionID)
);
CREATE TABLE IF NOT EXISTS yarn_version_meta
(
    VersionID   char(36)      NOT NULL,
    VersionType char(36)      NOT NULL,
    Changelog   varchar(5000) NULL,
    PRIMARY KEY (VersionID)
);

CREATE TABLE IF NOT EXISTS yarn_version_datacat_version_link
(
    VersionID        char(36) NOT NULL,
    DatacatVersionID char(36) NOT NULL,
    RelationType     char(36) NOT NULL,
    INDEX (VersionID, DatacatVersionID)
);

CREATE TABLE IF NOT EXISTS yarn_version_relation_types
(
    RelationTypeID   char(36)    NOT NULL PRIMARY KEY,
    RelationTypeName varchar(20) NOT NULL,
    INDEX (RelationTypeName)
);

CREATE TABLE IF NOT EXISTS yarn_version_types
(
    VersionTypeID   char(36)    NOT NULL PRIMARY KEY,
    VersionTypeName varchar(20) NOT NULL,
    INDEX (VersionTypeName)
);

CREATE TABLE IF NOT EXISTS yarn_relations
(
    YarnID         char(36) NOT NULL PRIMARY KEY,
    RelationTypeID char(36) NOT NULL,
    RelatedYarnID  char(36) NOT NULL,
    INDEX (YarnID, RelationTypeID, RelatedYarnID)
);

CREATE TABLE IF NOT EXISTS file_types
(
    FileTypeID    char(10)    NOT NULL PRIMARY KEY,
    FileTypeName  varchar(20) NOT NULL,
    MimeType      varchar(20) NOT NULL,
    FileExtension varchar(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS account_permissions
(
    AccountID      char(36)    NOT NULL,
    PermissionName varchar(70) NOT NULL,
    INDEX (AccountID, PermissionName)
);

CREATE TABLE IF NOT EXISTS account_details
(
    AccountID char(36)     NOT NULL,
    AboutMe   varchar(100) NULL,
    SubTitle  varchar(50)  NULL
);

CREATE TABLE IF NOT EXISTS link_type
(
    LinkID      char(36)     NOT NULL DEFAULT UUID(),
    DisplayName varchar(30)  NOT NULL,
    IconName    varchar(50)  NOT NULL,
    UrlRegex    varchar(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS account_links
(
    AccountID char(36)     NOT NULL,
    LinkID    char(36)     NOT NULL,
    LinkUrl   varchar(300) NOT NULL
);

INSERT INTO link_type (DisplayName, IconName, UrlRegex)
VALUES ('GitHub', 'fa-github', '^https:\/\/github\.com\/[A-Za-z0-9-]+\/?$'),
       ('Reddit', 'fa-reddit', '^https:\/\/(www\.)?reddit\.com\/(user\/)?[A-Za-z0-9_-]+\/?$'),
       ('Facebook', 'fa-facebook', '^https:\/\/(www\.)?facebook\.com\/[A-Za-z0-9.]+\/?$'),
       ('Stack Overflow', 'fa-stack-overflow', '^https:\/\/stackoverflow\.com\/users\/[0-9]+\/[A-Za-z0-9-]+\/?$'),
       ('Twitch', 'fa-twitch', '^https:\/\/(www\.)?twitch\.tv\/[A-Za-z0-9_]+\/?$'),
       ('YouTube', 'fa-youtube',
        '^https:\/\/(www\.)?youtube\.com\/(@[A-Za-z0-9_-]+|user\/[A-Za-z0-9_-]+|channel\/[A-Za-z0-9_-]+)\/?$'),
       ('Website', 'fa-globe', '^https?:\/\/([A-Za-z0-9-]+\.)+[A-Za-z]{2,}(\/.*)?$');

alter table account_details
    add DisplayName varchar(40) null;

ALTER TABLE account_links
    ADD UNIQUE KEY uk_account_link (AccountID, LinkID);