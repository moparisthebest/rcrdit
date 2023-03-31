SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;


CREATE TABLE IF NOT EXISTS `autorecs` (
  `autorec_id` int(11) NOT NULL AUTO_INCREMENT,
  `profile_id` int(11) NOT NULL,
  `priority` int(11) NOT NULL DEFAULT '5',
  `title` varchar(64) DEFAULT NULL,
  `channel_name` varchar(64) DEFAULT NULL,
  `days_of_week` varchar(64) DEFAULT NULL,
  `between_time_start` time DEFAULT NULL,
  `between_time_end` time DEFAULT NULL,
  `time_min` timestamp NULL DEFAULT NULL,
  `time_max` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`autorec_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `profile` (
  `profile_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `folder` varchar(64) NOT NULL,
  `run_at_recording_start` varchar(64) DEFAULT NULL,
  `run_at_recording_finish` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`profile_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
