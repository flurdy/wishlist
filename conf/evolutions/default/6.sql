# --- Sixth database schema

# --- !Ups


CREATE TABLE wishentry (
	wishid		BIGINT REFERENCES wish (wishid) ON DELETE CASCADE,
	wishlistid		BIGINT REFERENCES wishlist (wishlistid) ON DELETE CASCADE,
	ordinal		INT
);

INSERT INTO wishentry (wishid,wishlistid,ordinal)
	SELECT wishid,wishlistid,ordinal FROM wish;


# --- !Downs

 
DROP TABLE IF EXISTS wishentry;

