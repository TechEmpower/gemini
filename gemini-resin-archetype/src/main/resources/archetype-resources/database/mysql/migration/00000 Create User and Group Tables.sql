/*
 * This is a sample database migration script. Scripts should be named like so:
 *
 *   <task number> <task title>.sql
 *
 * It should contain SQL to apply the change from the task.
 *
 * For example:
 *
 *   ALTER TABLE foo ADD bar INTEGER NOT NULL DEFAULT 0
 *
 * Do not include SQL to undo the change from the task unless your project likes
 * living dangerously.
 *
 * Do not modify any of these scripts once they have been pushed to the central
 * git repository. If you need to make additional SQL changes related to a
 * task, create a new task for that and put the changes in a new script file
 * named after that new task.
 */

/*
 * Data definition script
 *
 * MySQL version
 *
 * This script provides the standard Gemini/Pyxis web application
 * tables for Users and User Groups.
 *
 * Please note that the selection of InnoDB for the table type
 * may not be appropriate for your application.  Consider using
 * MyISAM as needed.
 */

DROP TABLE IF EXISTS `group`;
CREATE TABLE `group` (
  `id` int(10) unsigned NOT NULL,
  `type` int(1) NOT NULL default '1',
  `name` varchar(50) default NULL,
  `description` varchar(50) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `usertogroup`;
CREATE TABLE `usertogroup` (
  `userid` int(10) unsigned NOT NULL default '0',
  `groupid` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`userid`,`groupid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `user`;
CREATE TABLE  `user` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `userusername` varchar(100) NOT NULL default '',
  `userfirstname` varchar(25) default NULL,
  `userlastname` varchar(25) default NULL,
  `useremail` varchar(254) default NULL,
  `userpassword` varchar(255) NOT NULL default '',
  `emailverificationticket` varchar(15) default NULL,
  `emailverificationdate` datetime default NULL,
  `passwordresetticket` varchar(255) default NULL,
  `passwordresetexpiration` datetime default NULL,
  `enabled` tinyint(1) NOT NULL,
  `userlastlogin` datetime default NULL,
  `userlastpasswordchange` datetime default NULL,
  `uservalidationhash` VARCHAR(255) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `logintoken`;
CREATE TABLE `logintoken` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `tokenhash` VARCHAR(100) NOT NULL,
  `created` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `username` (`username`) COMMENT 'Facilitate lookups by username.'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
