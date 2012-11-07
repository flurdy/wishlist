# --- Ninth database schema

# --- !Ups


CREATE TABLE emailverification (
	recipientid		BIGINT UNIQUE REFERENCES recipient (recipientid) ON DELETE CASCADE,
    email               VARCHAR(100),
    verificationhash     VARCHAR(100),
    verified    boolean default false
);



# --- !Downs


DROP TABLE IF EXISTS emailverification;
