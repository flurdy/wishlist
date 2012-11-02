# --- Eighth database schema

# --- !Ups


alter table wish add column recipientid BIGINT;

update wish set recipientid =
  (select wl.recipientid from wishentry we
    inner join wishlist wl on wl.wishlistid = we.wishlistid
  where we.wishid = wish.wishid);

ALTER TABLE wish ADD FOREIGN KEY (recipientid) REFERENCES recipient ON DELETE CASCADE;

# --- !Downs


alter table wish drop column recipientid;
