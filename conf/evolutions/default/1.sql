# --- First database schema

# --- !Ups


CREATE SEQUENCE recipient_seq START WITH 1000;
CREATE SEQUENCE wishlist_seq START WITH 1000;
CREATE SEQUENCE wish_seq START WITH 1000;


CREATE TABLE recipient (
    recipientid         SERIAL PRIMARY KEY,
    username              VARCHAR(100) UNIQUE,
    fullname              VARCHAR(100),
    email                 VARCHAR(100) NOT NULL,
    password              VARCHAR(100) NOT NULL
);

CREATE TABLE wishlist (
	wishlistid 		SERIAL PRIMARY KEY,
	title				VARCHAR(100) NOT NULL,
	description		VARCHAR(2000),
	recipientid		BIGINT NOT NULL
);

CREATE TABLE wish (
	wishid 		SERIAL PRIMARY KEY,
	title				VARCHAR(100) NOT NULL,
	description		VARCHAR(2000),
	wishlistid		BIGINT NOT NULL
);


# --- !Downs

DROP TABLE IF EXISTS recipient;
DROP TABLE IF EXISTS wishlist;
DROP TABLE IF EXISTS wish;

DROP SEQUENCE IF EXISTS recipient_seq;
DROP SEQUENCE IF EXISTS wishlist_seq;
DROP SEQUENCE IF EXISTS wish_seq;
