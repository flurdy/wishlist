# --- thirteenth database schema

# --- !Ups

alter table wishlink alter column url type VARCHAR(250);

# --- !Downs

alter table wishlink alter column url type VARCHAR(100);
