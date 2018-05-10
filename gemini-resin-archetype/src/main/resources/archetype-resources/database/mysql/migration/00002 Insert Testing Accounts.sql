/* Inserts some testing user accounts.  Delete these in a production environment. */

/* Username / Password: user / test */
INSERT INTO `user` (userusername, userfirstname, userlastname, useremail, userpassword, enabled) VALUES ('user', 'first', 'last', 'none-user@techempower.com', '$2a$12$OepZj8bxnM9vrVHr1x7/k.7n3vEPrprXRUBF.Ib1.3gdWbM6zEkdG', 1);
/* Username / Password: admin / test */
INSERT INTO `user` (userusername, userfirstname, userlastname, useremail, userpassword, enabled) VALUES ('admin', 'first', 'last', 'none-admin@techempower.com', '$2a$12$OepZj8bxnM9vrVHr1x7/k.7n3vEPrprXRUBF.Ib1.3gdWbM6zEkdG', 1);

INSERT INTO `usertogroup` (userid, groupid) VALUES (1, 1);
INSERT INTO `usertogroup` (userid, groupid) VALUES (2, 1);
INSERT INTO `usertogroup` (userid, groupid) VALUES (2, 1000);