# --- Fifth database schema

# --- !Ups

CREATE SEQUENCE reservation_seq START WITH 4000;


CREATE TABLE reservation (
	reservationid 	SERIAL PRIMARY KEY,
	recipientid		BIGINT REFERENCES recipient (recipientid) ON DELETE CASCADE,
	wishid			BIGINT REFERENCES wish (wishid) ON DELETE CASCADE
);



# --- !Downs

DROP TABLE IF EXISTS reservation;

DROP SEQUENCE IF EXISTS reservation_seq;
