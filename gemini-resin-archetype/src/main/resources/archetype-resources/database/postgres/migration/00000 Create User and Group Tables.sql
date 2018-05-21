CREATE TABLE public.group (
  id BIGSERIAL PRIMARY KEY,
  type INT NOT NULL default 0,
  name TEXT default NULL,
  description TEXT default NULL
);

CREATE TABLE public.UserToGroup (
  userId BIGINT NOT NULL,
  groupId BIGINT NOT NULL,
  PRIMARY KEY (userId,groupId)
);

CREATE TABLE public.user (
  id BIGSERIAL PRIMARY KEY,
  userusername TEXT NOT NULL default '',
  userfirstname TEXT default NULL,
  userlastname TEXT default NULL,
  useremail TEXT default NULL,
  userpassword TEXT NOT NULL default '',
  emailverificationticket TEXT default NULL,
  emailverificationdate TIMESTAMP default NULL,
  passwordresetticket TEXT default NULL,
  passwordresetexpiration TIMESTAMP default NULL,
  enabled BOOLEAN NOT NULL,
  userlastlogin TIMESTAMP default NULL,
  userlastpasswordchange TIMESTAMP default NULL,
  uservalidationhash TEXT default NULL
);

CREATE TABLE public.LoginToken (
  id BIGSERIAL PRIMARY KEY,
  username TEXT NOT NULL,
  tokenHash TEXT NOT NULL,
  created TIMESTAMP NOT NULL
);