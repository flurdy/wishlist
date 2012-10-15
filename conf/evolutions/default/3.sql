# --- First database schema

# --- !Ups


alter table wish add column ordinal int;


# --- !Downs

alter table wish drop column ordinal;

