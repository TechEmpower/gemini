ALTER TABLE `user` 
DROP COLUMN `uservalidationhash`;

DROP TABLE IF EXISTS `usertologin`;
CREATE TABLE `usertologin` (
  `userid` int(10) unsigned NOT NULL,
  `loginid` int(10) unsigned NOT NULL,
  PRIMARY KEY (`userid`, `loginid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `login`;
CREATE TABLE `login` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `validationhash` TEXT NULL,
  `created` DATETIME(3) NULL,
  `ipaddress` VARCHAR(45) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
