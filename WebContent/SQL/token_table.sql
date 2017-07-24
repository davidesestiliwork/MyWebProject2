CREATE DATABASE `mywebprojectdb` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE mywebprojectdb;

CREATE TABLE `token_table` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `token` varchar(45) NOT NULL,
  `used` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token_UNIQUE` (`token`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
