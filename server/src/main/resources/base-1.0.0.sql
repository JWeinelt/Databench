USE DataCat;


CREATE TABLE accounts (UserID char(36) NOT NULL,
                       eMail varchar(150) NOT NULL,
                       Username varchar(20) NOT NULL,
                       PasswordHashed VARCHAR(MAX) NOT NULL,
                       Created BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()),
                       LastLogin BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()),
                       AccountStatus char(36) NOT NULL,
                       VerificationStatus char(36) NOT NULL,
                       PRIMARY KEY (UserID,
                                    eMail,
                                    Username));


CREATE TABLE accounts_name_history (UserID char(36) NOT NULL,
                                    ChangeDate BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()),
                                    OldName varchar(20) NOT NULL,
                                    NewName varchar(20) NOT NULL);


CREATE TABLE accounts_verification_statuses (StatusID char(36) NOT NULL,
                                             StatusName varchar(20) NOT NULL,
                                             Color char(15) NOT NULL DEFAULT '000;000;000;100',
                                             PRIMARY KEY (StatusID,
                                                          StatusName));


CREATE TABLE accounts_account_statuses (StatusID char(36) NOT NULL,
                                        StatusName varchar(20) NOT NULL,
                                        Color char(15) NOT NULL DEFAULT '000;000;000;100');


CREATE TABLE yarn_status (StatusID char(36) NOT NULL PRIMARY KEY,
                          Name varchar(20) NOT NULL,
                          BgColor char(15) NOT NULL DEFAULT '000;000;000;100',
                          FgColor char(15) NOT NULL DEFAULT '000;000;000;100',
                          PublicVisible TINYINT NOT NULL DEFAULT 0);


INSERT INTO yarn_status (StatusID, Name, BgColor, FgColor, PublicVisible)
VALUES (NEWID(), 'Draft', '107;114;128;100', '107;114;128;20', 0),
       (NEWID(), 'Under Review', '201;151;000;100', '201;151;000;20', 0),
       (NEWID(), 'Published', '046;125;050;100', '046;125;050;20', 0),
       (NEWID(), 'Rejected', '176;064;064;100', '176;064;064;20', 0),
       (NEWID(), 'Deleted', '090;090;090;100', '090;090;090;20', 0);


CREATE TABLE yarn_meta (YarnID char(36) NOT NULL PRIMARY KEY,
                        YarnName varchar(50) NOT NULL,
                        AuthorID char(36) NOT NULL INDEX yarn_meta_author_idx,
                        Created BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()),
                        LastUpdated BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()),
                        Status char(36) NOT NULL,
                        LongDescription nvarchar(5000) NOT NULL,
                        ShortDescription nvarchar(100) NOT NULL,
                        FeatureHighlight1 varchar(30) NOT NULL,
                        FeatureHighlight2 varchar(30) NOT NULL,
                        FeatureHighlight3 varchar(30) NOT NULL,
                        WikiLink varchar(100) NOT NULL,
                        DiscordLink varchar(100) NOT NULL,
                        SourceLink varchar(100) NOT NULL,
                        BannerID char(36) NULL);


CREATE TABLE yarn_additional_links (YarnID char(36) NOT NULL,
                                    LinkIcon varchar(50) NOT NULL,
                                    LinkURL varchar(100) NOT NULL);


CREATE TABLE yarn_meta_tags_assigned (YarnID char(36) NOT NULL,
                                      TagID char(36) NOT NULL,
                                      DisplayPriority INT NOT NULL DEFAULT 0);


CREATE TABLE yarn_meta_tags (TagID char(36) NOT NULL PRIMARY KEY,
                             TagName varchar(30) NOT NULL,
                             TagColor char(15) NOT NULL DEFAULT '000;000;000;100');


CREATE TABLE yarn_images (YarnID char(36) NOT NULL,
                          ImageID char(36) NOT NULL,
                          GalleryOrder INT NOT NULL DEFAULT 0,
                          AltText varchar(100) NOT NULL);


CREATE TABLE yarn_live_data (YarnID char(36) NOT NULL, Date BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()),
                             Downloads INT NOT NULL DEFAULT 0,
                             VIEWS INT NOT NULL DEFAULT 0,
                             PRIMARY KEY (YarnID));


CREATE TABLE yarn_files (YarnID char(36) NOT NULL,
                         FileID char(36) NOT NULL,
                         FileType char(10) NOT NULL,
                         FileSize BIGINT NOT NULL,
                         CreationDate BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()));


CREATE TABLE yarn_versions (YarnID CHAR(36) NOT NULL,
                            VersionID CHAR(36) NOT NULL,
                            VersionName VARCHAR(20) NOT NULL,
                            CreationDate BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()));


CREATE TABLE yarn_version_meta (VersionID char(36) NOT NULL,
                                VersionType char(36) NOT NULL,
                                Changelog varchar(5000) NULL,
                                PRIMARY KEY (VersionID));


CREATE TABLE yarn_version_datacat_version_link (VersionID char(36) NOT NULL,
                                                DatacatVersionID char(36) NOT NULL,
                                                RelationType char(36) NOT NULL);


CREATE TABLE yarn_version_relation_types (RelationTypeID char(36) NOT NULL PRIMARY KEY,
                                          RelationTypeName varchar(20) NOT NULL);


CREATE TABLE yarn_version_types (VersionTypeID char(36) NOT NULL PRIMARY KEY,
                                 VersionTypeName varchar(20) NOT NULL);


CREATE TABLE yarn_relations (YarnID char(36) NOT NULL PRIMARY KEY,
                             RelationTypeID char(36) NOT NULL,
                             RelatedYarnID char(36) NOT NULL);


CREATE TABLE file_types (FileTypeID char(10) NOT NULL PRIMARY KEY,
                         FileTypeName varchar(20) NOT NULL,
                         MimeType varchar(20) NOT NULL,
                         FileExtension varchar(10) NOT NULL);


CREATE TABLE account_permissions (AccountID char(36) NOT NULL,
                                  PermissionName varchar(70) NOT NULL);


CREATE TABLE account_details (AccountID char(36) NOT NULL,
                              AboutMe varchar(100) NULL,
                              SubTitle varchar(50) NULL,
                              DisplayName varchar(40) NULL);


CREATE TABLE link_type (LinkID char(36) NOT NULL DEFAULT NEWID(),
                        DisplayName varchar(30) NOT NULL,
                        IconName varchar(50) NOT NULL,
                        UrlRegex varchar(100) NOT NULL);


CREATE TABLE account_links (AccountID char(36) NOT NULL,
                            LinkID char(36) NOT NULL,
                            LinkUrl varchar(300) NOT NULL);


INSERT INTO link_type (DisplayName, IconName, UrlRegex)
VALUES ('GitHub', 'fa-github', '^https:\/\/github\.com\/[A-Za-z0-9-]+\/?$'),
       ('Reddit', 'fa-reddit', '^https:\/\/(www\.)?reddit\.com\/(user\/)?[A-Za-z0-9_-]+\/?$'),
       ('Facebook', 'fa-facebook', '^https:\/\/(www\.)?facebook\.com\/[A-Za-z0-9.]+\/?$'),
       ('Stack Overflow', 'fa-stack-overflow', '^https:\/\/stackoverflow\.com\/users\/[0-9]+\/[A-Za-z0-9-]+\/?$'),
       ('Twitch', 'fa-twitch', '^https:\/\/(www\.)?twitch\.tv\/[A-Za-z0-9_]+\/?$'),
       ('YouTube', 'fa-youtube', '^https:\/\/(www\.)?youtube\.com\/(@[A-Za-z0-9_-]+|user\/[A-Za-z0-9_-]+|channel\/[A-Za-z0-9_-]+)\/?$'),
       ('Website', 'fa-globe', '^https?:\/\/([A-Za-z0-9-]+\.)+[A-Za-z]{2,}(\/.*)?$');