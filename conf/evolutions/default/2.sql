# --- First database schema

# --- !Ups


insert into recipient (recipientid,username,fullname,email,password) 
	values ((select NEXTVAL('recipient_seq')),'testuser','Test user','test@example.com','$2a$10$.LMFHFigeUeZwg3VZgZr3ekcKF6xFjjcnfPPxlTwJ0MAsdPGxXf8y');


insert into wishlist (wishlistid,title,description,recipientid)
	values ((select NEXTVAL('wishlist_seq')),'Christmas list for Ivar','',(select dreamerid from dreamer where username='testuser'));

insert into wishlist (wishlistid,title,description,recipientid)
	values ((select NEXTVAL('wishlist_seq')),'Main list for Xerxes','',(select dreamerid from dreamer where username='testuser'));


# --- !Downs

delete from recipient where username = 'testuser';

delete from wishlist where title = 'Christmas list for Ivar';
delete from wishlist where title = 'Main list for Xerces';