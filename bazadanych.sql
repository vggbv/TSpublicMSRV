CREATE DATABASE projekt;

USE projekt;

CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL
);

CREATE TABLE posts (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       user INT NOT NULL,
                       content TEXT NOT NULL,
                       tstamp timestamp NOT NULL DEFAULT current_timestamp(),
                       FOREIGN KEY (user) REFERENCES users(id)
);