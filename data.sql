CREATE DATABASE `transportapp`;

use transportapp;

CREATE TABLE `chauffeur` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`nom` varchar(255) NOT NULL,
`prenom` varchar(255) NOT NULL,
`email` varchar(255) NOT NULL,
`telephone` varchar(255) DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `email` (`email`));

CREATE TABLE `client` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`nom` varchar(255) NOT NULL,
`prenom` varchar(255) NOT NULL,
`email` varchar(255) NOT NULL,
`telephone` varchar(255) DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `email` (`email`));

CREATE TABLE `course` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`lieu_depart` varchar(255) NOT NULL,
`lieu_arrivee` varchar(255) NOT NULL,
`prix` double DEFAULT NULL,
`date_depart` varchar(255) DEFAULT NULL,
`completed` tinyint(1) DEFAULT 0,
`chauffeur_id` bigint(20) DEFAULT NULL,
`client_id` bigint(20) DEFAULT NULL,
`starttime` varchar(25) DEFAULT NULL,
PRIMARY KEY (`id`),
KEY `chauffeur_id` (`chauffeur_id`),
KEY `client_id` (`client_id`),
CONSTRAINT `course_ibfk_1` FOREIGN KEY (`chauffeur_id`) REFERENCES `chauffeur` (`id`),
CONSTRAINT `course_ibfk_2` FOREIGN KEY (`client_id`) REFERENCES `client` (`id`));

CREATE TABLE `qrcode` (`id` bigint(20) NOT NULL AUTO_INCREMENT,
`client_id` bigint(20) DEFAULT NULL,
`chauffeur_id` bigint(20) DEFAULT NULL,
`course_id` bigint(20) DEFAULT NULL,
`content` varchar(255) DEFAULT NULL,
PRIMARY KEY (`id`),
KEY `client_id` (`client_id`),
KEY `chauffeur_id` (`chauffeur_id`),
KEY `course_id` (`course_id`),
CONSTRAINT `qrcode_ibfk_1` FOREIGN KEY (`client_id`) REFERENCES `client` (`id`),
CONSTRAINT `qrcode_ibfk_2` FOREIGN KEY (`chauffeur_id`) REFERENCES `chauffeur` (`id`),
CONSTRAINT `qrcode_ibfk_3` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`));
