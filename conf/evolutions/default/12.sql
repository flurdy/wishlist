# --- eleventh database schema

# --- !Ups

insert into emailverification (recipientid,email,verificationhash,verified) values (
	(select recipientid from recipient where username = 'testuser'),
	'test@example.com','',true),
	((select recipientid from recipient where username = 'anotheruser'),
	'test@example.com','',true);




# --- !Downs

