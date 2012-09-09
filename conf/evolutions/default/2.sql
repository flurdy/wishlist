# --- First database schema

# --- !Ups


insert into dreamer (dreamerid,username,fullname,email,password) 
	values ((select NEXTVAL('dreamer_seq')),'testuser','Test user','test@example.com','$2a$10$.LMFHFigeUeZwg3VZgZr3ekcKF6xFjjcnfPPxlTwJ0MAsdPGxXf8y');


# --- !Downs

DELETE FROM dreamer WHERE username = 'testuser':