ALTER TABLE public.user DROP COLUMN uservalidationhash;

CREATE TABLE public.usertologin (
  userid BIGSERIAL NOT NULL,
  loginid BIGSERIAL NOT NULL,
  PRIMARY KEY (userid, loginid)
);

CREATE TABLE public.login (
  id BIGSERIAL NOT NULL,
  validationhash TEXT NULL,
  created TIMESTAMP NULL,
  ipaddress TEXT NULL,
  PRIMARY KEY (id)
);
