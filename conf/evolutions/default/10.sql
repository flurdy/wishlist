# --- tenth database schema

# --- !Ups


CREATE TABLE wishlistorganiser (
	wishlistid		BIGINT REFERENCES wishlist (wishlistid) ON DELETE CASCADE,
	recipientid		BIGINT REFERENCES recipient (recipientid) ON DELETE CASCADE
);

# --- !Downs



DROP TABLE IF EXISTS wishlistorganiser;
