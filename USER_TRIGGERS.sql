
-- CREATE USER

CREATE PROCEDURE spCreateUser
    @fullname NVARCHAR(100),
    @email NVARCHAR(255),
    @phoneNumber NVARCHAR(15),
    @address NVARCHAR(255),
    @privilege NVARCHAR(10),
    @status NVARCHAR(10)
AS
BEGIN
    BEGIN TRY
        INSERT INTO USERS (fullname, email, phoneNumber, address, privellege, status)
        VALUES (@fullname, @email, @phoneNumber, @address, @privilege, @status);

        PRINT 'User created successfully.';
    END TRY
    BEGIN CATCH
        PRINT 'Error: Unable to create user. ' + ERROR_MESSAGE();
    END CATCH
END;


EXEC spCreateUser 
    @fullname = 'John Doe', 
    @email = 'john@example.com', 
    @phoneNumber = '1234567890', 
    @address = '123 Street, City', 
    @privilege = 'client', 
    @status = 'active';

	SELECT* FROM USERS;



-- GET USER BY EMAIL 
CREATE PROCEDURE spGetUsers
AS
BEGIN
    BEGIN TRY
        SELECT * FROM USERS;
        PRINT 'Users retrieved successfully.';
    END TRY
    BEGIN CATCH
        PRINT 'Error: Unable to retrieve users. ' + ERROR_MESSAGE();
    END CATCH
END;

EXEC spGetUsers;


-- UPDATE USER

CREATE PROCEDURE spUpdateUser
    @userID INT,
    @fullname NVARCHAR(100),
    @email NVARCHAR(255),
    @phoneNumber NVARCHAR(15),
    @address NVARCHAR(255),
    @privilege NVARCHAR(10),
    @status NVARCHAR(10)
AS
BEGIN
    BEGIN TRY
        UPDATE USERS
        SET fullname = @fullname, 
            email = @email, 
            phoneNumber = @phoneNumber, 
            address = @address, 
            privellege = @privilege, 
            status = @status
        WHERE userID = @userID;

        IF @@ROWCOUNT = 0
            PRINT 'No user found with the given ID.';
        ELSE
            PRINT 'User updated successfully.';
    END TRY
    BEGIN CATCH
        PRINT 'Error: Unable to update user. ' + ERROR_MESSAGE();
    END CATCH
END;


EXEC spUpdateUser 
    @userID = 1, 
    @fullname = 'John Smith', 
    @email = 'john.smith@example.com', 
    @phoneNumber = '0987654321', 
    @address = '456 Avenue, City', 
    @privilege = 'admin', 
    @status = 'active';


	-- DELETE USERS


	CREATE PROCEDURE spDeleteUser
    @userID INT
AS
BEGIN
    BEGIN TRY
        DELETE FROM USERS WHERE userID = @userID;

        IF @@ROWCOUNT = 0
            PRINT 'No user found with the given ID.';
        ELSE
            PRINT 'User deleted successfully.';
    END TRY
    BEGIN CATCH
        PRINT 'Error: Unable to delete user. ' + ERROR_MESSAGE();
    END CATCH
END;


EXEC spDeleteUser 
    @userID = 1;



