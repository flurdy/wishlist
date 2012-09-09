# --- First database schema

# --- !Ups


CREATE SEQUENCE dreamer_seq START WITH 1000;


CREATE TABLE dreamer (
    dreamerid         SERIAL PRIMARY KEY,
    username              VARCHAR(100) UNIQUE,
    fullname              VARCHAR(100),
    email                 VARCHAR(100),
    password              VARCHAR(100) NOT NULL
);


# --- !Downs

DROP TABLE IF EXISTS dreamer;

DROP SEQUENCE IF EXISTS dreamer_seq;
