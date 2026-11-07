-- Client Files - stored procedures. Safe to run repeatedly.
-- Business-rule failures use THROW with a 5xxxx number: the application shows those
-- messages to the user as they are, so they must stay readable.

USE [$(DB)];
GO

------------------------------------------------------------------------------- Users

CREATE OR ALTER PROCEDURE dbo.spHasAdmin
AS
BEGIN
    SET NOCOUNT ON;
    SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.USERS WHERE privilege = 'admin') THEN 1 ELSE 0 END;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spCreateClient
    @fullName NVARCHAR(100), @email NVARCHAR(255), @phoneNumber NVARCHAR(30),
    @address NVARCHAR(255), @passwordHash VARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        INSERT INTO dbo.USERS (fullName, email, phoneNumber, address, passwordHash, privilege)
        VALUES (@fullName, @email, @phoneNumber, @address, @passwordHash, 'client');
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() IN (2601, 2627)
            THROW 50001, 'An account with this email already exists.', 1;
        THROW;
    END CATCH;
END;
GO

-- Creates the first administrator; refuses as soon as one exists.
CREATE OR ALTER PROCEDURE dbo.spCreateFirstAdmin
    @fullName NVARCHAR(100), @email NVARCHAR(255), @passwordHash VARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM dbo.USERS WITH (UPDLOCK, HOLDLOCK) WHERE privilege = 'admin')
    BEGIN
        ROLLBACK;
        THROW 50002, 'An administrator already exists.', 1;
    END;
    INSERT INTO dbo.USERS (fullName, email, passwordHash, privilege)
    VALUES (@fullName, @email, @passwordHash, 'admin');
    COMMIT;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spGetUserByEmail
    @email NVARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT userID, fullName, email, phoneNumber, address, passwordHash, privilege, status, createdAt
    FROM dbo.USERS WHERE email = @email;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spGetPasswordHash
    @userID INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT passwordHash FROM dbo.USERS WHERE userID = @userID;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spUpdateProfile
    @userID INT, @fullName NVARCHAR(100), @email NVARCHAR(255),
    @phoneNumber NVARCHAR(30), @address NVARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        UPDATE dbo.USERS
        SET fullName = @fullName, email = @email, phoneNumber = @phoneNumber, address = @address
        WHERE userID = @userID;
        IF @@ROWCOUNT = 0
            THROW 50003, 'This account no longer exists.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() IN (2601, 2627)
            THROW 50001, 'An account with this email already exists.', 1;
        THROW;
    END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spChangePassword
    @userID INT, @passwordHash VARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE dbo.USERS SET passwordHash = @passwordHash WHERE userID = @userID;
    IF @@ROWCOUNT = 0
        THROW 50003, 'This account no longer exists.', 1;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spGetClients
    @adminID INT
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT EXISTS (SELECT 1 FROM dbo.USERS WHERE userID = @adminID AND privilege = 'admin' AND status = 'active')
        THROW 50004, 'Only an administrator can do this.', 1;

    SELECT u.userID, u.fullName, u.email, u.phoneNumber, u.address, u.status, u.createdAt,
           COUNT(f.docID) AS documents,
           COALESCE(SUM(CASE WHEN f.status = 'pending' THEN 1 ELSE 0 END), 0) AS pending,
           MAX(f.submittedAt) AS lastSubmission
    FROM dbo.USERS u
    LEFT JOIN dbo.FILES f ON f.clientID = u.userID
    WHERE u.privilege = 'client'
    GROUP BY u.userID, u.fullName, u.email, u.phoneNumber, u.address, u.status, u.createdAt
    ORDER BY u.fullName;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spSetClientStatus
    @adminID INT, @clientID INT, @status NVARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT EXISTS (SELECT 1 FROM dbo.USERS WHERE userID = @adminID AND privilege = 'admin' AND status = 'active')
        THROW 50004, 'Only an administrator can do this.', 1;

    UPDATE dbo.USERS SET status = @status WHERE userID = @clientID AND privilege = 'client';
    IF @@ROWCOUNT = 0
        THROW 50003, 'This account no longer exists.', 1;
END;
GO

------------------------------------------------------------------------------- Documents

CREATE OR ALTER PROCEDURE dbo.spCreateFile
    @clientID INT, @docName NVARCHAR(255), @category NVARCHAR(50), @docType NVARCHAR(20),
    @expiringDate DATE, @supervisorName NVARCHAR(100), @content VARBINARY(MAX)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    IF NOT EXISTS (SELECT 1 FROM dbo.USERS WHERE userID = @clientID AND privilege = 'client' AND status = 'active')
        THROW 50005, 'Only an active client account can submit documents.', 1;

    BEGIN TRANSACTION;
    INSERT INTO dbo.FILES (docName, category, docType, fileSize, expiringDate, supervisorName, clientID)
    VALUES (@docName, @category, @docType, DATALENGTH(@content), @expiringDate, @supervisorName, @clientID);

    DECLARE @docID INT = SCOPE_IDENTITY();
    INSERT INTO dbo.FILE_CONTENT (docID, content) VALUES (@docID, @content);
    COMMIT;
    SELECT @docID;
END;
GO

-- Administrators see every document, clients only their own.
CREATE OR ALTER PROCEDURE dbo.spGetFiles
    @actorID INT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @privilege NVARCHAR(10) =
        (SELECT privilege FROM dbo.USERS WHERE userID = @actorID AND status = 'active');
    IF @privilege IS NULL
        THROW 50006, 'This account is no longer active.', 1;

    SELECT f.docID, f.docName, f.category, f.docType, f.fileSize, f.submittedAt, f.expiringDate,
           f.supervisorName, f.status, f.reviewNote, f.reviewedAt,
           f.clientID, c.fullName AS clientName, c.email AS clientMail, r.fullName AS reviewerName
    FROM dbo.FILES f
    JOIN dbo.USERS c ON c.userID = f.clientID
    LEFT JOIN dbo.USERS r ON r.userID = f.reviewerID
    WHERE @privilege = 'admin' OR f.clientID = @actorID
    ORDER BY f.submittedAt DESC, f.docID DESC;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spGetFileContent
    @docID INT, @actorID INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT c.content
    FROM dbo.FILE_CONTENT c
    JOIN dbo.FILES f ON f.docID = c.docID
    JOIN dbo.USERS a ON a.userID = @actorID AND a.status = 'active'
    WHERE c.docID = @docID AND (a.privilege = 'admin' OR f.clientID = @actorID);
END;
GO

CREATE OR ALTER PROCEDURE dbo.spReviewFile
    @docID INT, @adminID INT, @status NVARCHAR(10), @reviewNote NVARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT EXISTS (SELECT 1 FROM dbo.USERS WHERE userID = @adminID AND privilege = 'admin' AND status = 'active')
        THROW 50004, 'Only an administrator can do this.', 1;
    IF @status NOT IN ('approved', 'rejected')
        THROW 50007, 'A review must approve or reject the document.', 1;

    UPDATE dbo.FILES
    SET status = @status, reviewNote = @reviewNote, reviewedAt = SYSDATETIME(), reviewerID = @adminID
    WHERE docID = @docID;
    IF @@ROWCOUNT = 0
        THROW 50008, 'This document no longer exists.', 1;
END;
GO

-- Approved documents are part of the record: their owner cannot delete them.
CREATE OR ALTER PROCEDURE dbo.spDeleteFile
    @docID INT, @actorID INT
AS
BEGIN
    SET NOCOUNT ON;
    DELETE FROM dbo.FILES WHERE docID = @docID AND clientID = @actorID AND status <> 'approved';
    IF @@ROWCOUNT = 0
        THROW 50009, 'This document cannot be deleted: it has been approved or no longer exists.', 1;
END;
GO
