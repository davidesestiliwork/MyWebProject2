CREATE DATABASE `mywebprojectdb` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE mywebprojectdb;

CREATE TABLE `token_table` (
  `token` varchar(45) NOT NULL,
  `used` int(11) NOT NULL,
  `base_dir` varchar(200) NOT NULL,
  PRIMARY KEY (`token`),
  UNIQUE KEY `token_UNIQUE` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
