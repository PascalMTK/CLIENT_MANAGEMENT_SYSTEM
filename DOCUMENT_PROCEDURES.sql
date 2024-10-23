-- INSERT FILE PR DOCUMENT

CREATE PROCEDURE spCreateFile
    @docName NVARCHAR(100),
    @category NVARCHAR(50),
    @docType NVARCHAR(50),
    @uploatedDate DATE,
    @expiringDate DATE,
    @submisionDate DATE,
    @supervisorName VARCHAR(255),
    @status NVARCHAR(10),
    @clientMail NVARCHAR(255),
    @clientName NVARCHAR(255),
    @adminName NVARCHAR(255),
    @adminMail NVARCHAR(255)
AS
BEGIN
    BEGIN TRY
        INSERT INTO FILES (docName, category, docType, uploatedDate, expiringDate, 
                           submisionDate, supervisorName, status, clientMail, 
                           clientName, adminName, adminMail)
        VALUES (@docName, @category, @docType, @uploatedDate, @expiringDate, 
                @submisionDate, @supervisorName, @status, @clientMail, 
                @clientName, @adminName, @adminMail);

        PRINT 'File created successfully.';
    END TRY
    BEGIN CATCH
        PRINT 'Error: Unable to create file. ' + ERROR_MESSAGE();
    END CATCH
END;


EXEC spCreateFile 
    @docName = 'Project Report', 
    @category = 'Report', 
    @docType = 'PDF', 
    @uploatedDate = '2024-10-21', 
    @expiringDate = '2025-10-21', 
    @submisionDate = '2024-10-22', 
    @supervisorName = 'John Doe', 
    @status = 'pending', 
    @clientMail = 'client@example.com', 
    @clientName = 'Jane Client', 
    @adminName = 'Admin User', 
    @adminMail = 'admin@example.com';



	-- SELECT ALL THE FILES 


	CREATE PROCEDURE spGetFiles
AS
BEGIN
    BEGIN TRY
        SELECT * FROM FILES;
        PRINT 'Files retrieved successfully.';
    END TRY
    BEGIN CATCH
        PRINT 'Error: Unable to retrieve files. ' + ERROR_MESSAGE();
    END CATCH
END;


EXEC spGetFiles;


-- UPDATE PROCEDURE

CREATE PROCEDURE spUpdateFile
    @docID INT,
    @docName NVARCHAR(100),
    @category NVARCHAR(50),
    @docType NVARCHAR(50),
    @uploatedDate DATE,
    @expiringDate DATE,
    @submisionDate DATE,
    @supervisorName VARCHAR(255),
    @status NVARCHAR(10),
    @clientMail NVARCHAR(255),
    @clientName NVARCHAR(255),
    @adminName NVARCHAR(255),
    @adminMail NVARCHAR(255)
AS
BEGIN
    BEGIN TRY
        UPDATE FILES
        SET docName = @docName,
            category = @category,
            docType = @docType,
            uploatedDate = @uploatedDate,
            expiringDate = @expiringDate,
            submisionDate = @submisionDate,
            supervisorName = @supervisorName,
            status = @status,
            clientMail = @clientMail,
            clientName = @clientName,
            adminName = @adminName,
            adminMail = @adminMail
        WHERE docID = @docID;

        IF @@ROWCOUNT = 0
            PRINT 'No file found with the given ID.';
        ELSE
            PRINT 'File updated successfully.';
    END TRY
    BEGIN CATCH
        PRINT 'Error: Unable to update file. ' + ERROR_MESSAGE();
    END CATCH
END;


EXEC spUpdateFile 
    @docID = 1,
    @docName = 'Updated Report', 
    @category = 'Updated Category', 
    @docType = 'Word', 
    @uploatedDate = '2024-10-20', 
    @expiringDate = '2025-10-20', 
    @submisionDate = '2024-10-23', 
    @supervisorName = 'Jane Supervisor', 
    @status = 'approved', 
    @clientMail = 'client@example.com', 
    @clientName = 'Jane Client', 
    @adminName = 'Admin User', 
    @adminMail = 'admin@example.com';



	-- DELETE 

	CREATE PROCEDURE spDeleteFile
    @docID INT
AS
BEGIN
    BEGIN TRY
        DELETE FROM FILES WHERE docID = @docID;

        IF @@ROWCOUNT = 0
            PRINT 'No file found with the given ID.';
        ELSE
            PRINT 'File deleted successfully.';
    END TRY
    BEGIN CATCH
        PRINT 'Error: Unable to delete file. ' + ERROR_MESSAGE();
    END CATCH
END;


EXEC spDeleteFile 
    @docID = 1;


