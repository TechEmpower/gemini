/*
 * This is a sample database migration script. Migrations will be applied in
 * order, so choose a naming scheme and stick with it, such as:
 *
 *   V<task number>__<task title>.sql
 *   OR
 *   V<timestamp>__<task title>.sql
 *
 * (Start with 'V' followed by a number then __ and no spaces in the name.)
 *
 * If you choose the task number scheme, that file should contain SQL to apply
 * the change from the task.
 *
 * For example:
 *
 *   ALTER TABLE foo ADD bar INTEGER NOT NULL DEFAULT 0
 *
 * Do not modify any of these scripts once they have been pushed to the central
 * git repository. If you need to make additional SQL changes related to a
 * task, create a new task for that and put the changes in a new script file
 * named after that new task.
 */

/*
 * Data definition script
 *
 * Postgres version
 *
 * This script provides the standard Gemini/Pyxis web application
 * tables for Users and User Groups.
 */

CREATE TABLE IF NOT EXISTS "group" (
  id BIGSERIAL PRIMARY KEY,
  "type" INTEGER NOT NULL default 1,
  name TEXT default NULL,
  description TEXT default NULL
);
COMMENT ON TABLE "group" IS 'User groups.';
COMMENT ON COLUMN "group".type IS 'Group type.';
COMMENT ON COLUMN "group".name IS 'Display name.';
COMMENT ON COLUMN "group".description IS 'Description.';


CREATE TABLE IF NOT EXISTS usertogroup (
  userid BIGINT NOT NULL,
  groupid BIGINT NOT NULL,
  PRIMARY KEY (userid,groupid)
);

CREATE TABLE IF NOT EXISTS "user" (
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
  userlastpasswordchange TIMESTAMP default NULL
);

CREATE TABLE IF NOT EXISTS logintoken (
  id BIGSERIAL PRIMARY KEY,
  username TEXT NOT NULL,
  tokenhash TEXT NOT NULL,
  created TIMESTAMP NOT NULL
);
COMMENT ON COLUMN logintoken.username IS 'Facilitate lookups by username.';
  
CREATE TABLE IF NOT EXISTS usertologin (
  userid BIGINT NOT NULL,
  loginid BIGINT NOT NULL,
  PRIMARY KEY (userid, loginid)
);

CREATE TABLE IF NOT EXISTS login (
  id BIGSERIAL PRIMARY KEY,
  validationhash TEXT NULL,
  created TIMESTAMP NULL,
  ipaddress TEXT NULL
);
