# --- eleventh database schema

# --- !Ups

CREATE SEQUENCE wishlink_seq START WITH 1000;
create table wishlink (
   linkid         SERIAL PRIMARY KEY,
	wishid			BIGINT REFERENCES wish (wishid) ON DELETE CASCADE,
   url            VARCHAR(100) NOT NULL
);


# --- !Downs


DROP TABLE IF EXISTS wishlink;

DROP SEQUENCE IF EXISTS wishlink_seq;
