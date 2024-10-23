CREATE DATABASE CLIENT_FILE_MANAGEMENT_SYSTEM;
USE CLIENT_FILE_MANAGEMENT_SYSTEM;

CREATE TABLE USERS (
    userID INT PRIMARY KEY IDENTITY(1,1) NOT NULL,
    fullname NVARCHAR(100) NOT NULL,
    email NVARCHAR(255) UNIQUE NOT NULL,
    phoneNumber NVARCHAR(15),
    address NVARCHAR(255),
    privellege NVARCHAR(10) CHECK (privellege IN ('admin', 'client')),
    status NVARCHAR(10) CHECK (status IN ('active', 'inactive'))
);


CREATE TABLE FILES (
    docID INT PRIMARY KEY IDENTITY(1,1),
    docName NVARCHAR(100) NOT NULL,
    category NVARCHAR(50),
    docType NVARCHAR(50),
    uploatedDate DATE,
    expiringDate DATE,
    submisionDate DATE,
	supervisorName VARCHAR(255),
    status NVARCHAR(10) CHECK (status IN ('pending', 'approved', 'rejected')),
    clientMail NVARCHAR(255) NOT NULL,
	clientName  NVARCHAR(255),
	adminName NVARCHAR(255),
	adminMail NVARCHAR(255),

);
