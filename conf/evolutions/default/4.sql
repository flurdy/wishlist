# --- First database schema

# --- !Ups


alter table recipient add column isadmin boolean default false;


# --- !Downs

alter table recipient drop column isadmin;

