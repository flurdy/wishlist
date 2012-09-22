# --- First database schema

# --- !Ups


insert into recipient (recipientid,username,fullname,email,password) 
	values ((select NEXTVAL('recipient_seq')),'testuser','Test user','test@example.com','$2a$10$.LMFHFigeUeZwg3VZgZr3ekcKF6xFjjcnfPPxlTwJ0MAsdPGxXf8y');

insert into recipient (recipientid,username,fullname,email,password) 
	values ((select NEXTVAL('recipient_seq')),'anotheruser','Another user','test@example.com','$2a$10$.LMFHFigeUeZwg3VZgZr3ekcKF6xFjjcnfPPxlTwJ0MAsdPGxXf8y');


insert into wishlist (wishlistid,title,description,recipientid)
	values ((select NEXTVAL('wishlist_seq')),'Christmas list','',(select recipientid from recipient where username='testuser'));

insert into wishlist (wishlistid,title,description,recipientid)
	values ((select NEXTVAL('wishlist_seq')),'Xerxes wishes','',(select recipientid from recipient where username='anotheruser'));

insert into wish (wishid,title,description,wishlistid)
	values ((select NEXTVAL('wish_seq')),'Red car','',(select wishlistid from wishlist where title='Christmas list'));

insert into wish (wishid,title,description,wishlistid)
	values ((select NEXTVAL('wish_seq')),'Blue watch','',(select wishlistid from wishlist where title='Christmas list'));


# --- !Downs

delete from recipient where username = 'testuser';

delete from wishlist where title = 'Christmas list for Ivar';
delete from wishlist where title = 'Main list for Xerces';