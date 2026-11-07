-- Client Files - database schema.
-- Safe to run repeatedly: existing tables and data are left untouched.
-- Run through setup-database.bat, or: sqlcmd -S <server> -E -b -v DB="<name>" -i sql\01_schema.sql

IF DB_ID(N'$(DB)') IS NULL
    CREATE DATABASE [$(DB)];
GO
USE [$(DB)];
GO

-- The first version of this schema had no password column and cannot be upgraded in place.
IF OBJECT_ID(N'dbo.USERS', N'U') IS NOT NULL AND COL_LENGTH(N'dbo.USERS', N'passwordHash') IS NULL
    THROW 50000, 'An older USERS table exists in this database. Back it up and drop it, or choose another database name.', 1;
GO

IF OBJECT_ID(N'dbo.USERS', N'U') IS NULL
CREATE TABLE dbo.USERS (
    userID       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_USERS PRIMARY KEY,
    fullName     NVARCHAR(100) NOT NULL,
    email        NVARCHAR(255) NOT NULL CONSTRAINT UQ_USERS_email UNIQUE,
    phoneNumber  NVARCHAR(30)  NULL,
    address      NVARCHAR(255) NULL,
    passwordHash VARCHAR(255)  NOT NULL,
    privilege    NVARCHAR(10)  NOT NULL CONSTRAINT CK_USERS_privilege CHECK (privilege IN ('admin', 'client')),
    status       NVARCHAR(10)  NOT NULL CONSTRAINT DF_USERS_status DEFAULT ('active')
                               CONSTRAINT CK_USERS_status CHECK (status IN ('active', 'inactive')),
    createdAt    DATETIME2(0)  NOT NULL CONSTRAINT DF_USERS_createdAt DEFAULT (SYSDATETIME())
);
GO

IF OBJECT_ID(N'dbo.FILES', N'U') IS NULL
CREATE TABLE dbo.FILES (
    docID          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_FILES PRIMARY KEY,
    docName        NVARCHAR(255) NOT NULL,
    category       NVARCHAR(50)  NOT NULL,
    docType        NVARCHAR(20)  NOT NULL,
    fileSize       BIGINT        NOT NULL,
    submittedAt    DATETIME2(0)  NOT NULL CONSTRAINT DF_FILES_submittedAt DEFAULT (SYSDATETIME()),
    expiringDate   DATE          NULL,
    supervisorName NVARCHAR(100) NULL,
    status         NVARCHAR(10)  NOT NULL CONSTRAINT DF_FILES_status DEFAULT ('pending')
                                 CONSTRAINT CK_FILES_status CHECK (status IN ('pending', 'approved', 'rejected')),
    reviewNote     NVARCHAR(500) NULL,
    reviewedAt     DATETIME2(0)  NULL,
    reviewerID     INT           NULL CONSTRAINT FK_FILES_reviewer REFERENCES dbo.USERS (userID),
    clientID       INT           NOT NULL CONSTRAINT FK_FILES_client REFERENCES dbo.USERS (userID)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_FILES_clientID' AND object_id = OBJECT_ID(N'dbo.FILES'))
    CREATE INDEX IX_FILES_clientID ON dbo.FILES (clientID) INCLUDE (status, submittedAt);
GO

-- Kept apart from FILES so that listing documents never reads their content.
IF OBJECT_ID(N'dbo.FILE_CONTENT', N'U') IS NULL
CREATE TABLE dbo.FILE_CONTENT (
    docID   INT NOT NULL CONSTRAINT PK_FILE_CONTENT PRIMARY KEY
            CONSTRAINT FK_FILE_CONTENT_file REFERENCES dbo.FILES (docID) ON DELETE CASCADE,
    content VARBINARY(MAX) NOT NULL
);
GO
