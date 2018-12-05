/* Inserts some testing user accounts.  Delete these in a production environment. */

/* Username / Password: user / test */
INSERT INTO "user" (userusername, userfirstname, userlastname, useremail, userpassword, enabled, userlastpasswordchange) VALUES ('user', 'first', 'last', 'none-user@techempower.com', '$2a$12$OepZj8bxnM9vrVHr1x7/k.6gE8QJZ8OKLTs7PPFhUopilS2RmQjq.', true, now());
/* Username / Password: admin / test */
INSERT INTO "user" (userusername, userfirstname, userlastname, useremail, userpassword, enabled, userlastpasswordchange) VALUES ('admin', 'first', 'last', 'none-admin@techempower.com', '$2a$12$OepZj8bxnM9vrVHr1x7/k.6gE8QJZ8OKLTs7PPFhUopilS2RmQjq.', true, now());

INSERT INTO usertogroup (userid, groupid) VALUES (1, 1);
INSERT INTO usertogroup (userid, groupid) VALUES (2, 1);
INSERT INTO usertogroup (userid, groupid) VALUES (2, 1000);
