-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: synchtask
-- ------------------------------------------------------
-- Server version	8.0.45-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `activities`
--

DROP TABLE IF EXISTS `activities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `type` enum('BOARD_COLLABORATORS_UPDATED','BOARD_CREATED','BOARD_DELETED','BOARD_UPDATED','PROJECT_BOARDS_UPDATED','PROJECT_CREATED','PROJECT_DELETED','PROJECT_UPDATED','TASK_ASSIGNED','TASK_ATTACHMENT_ADDED','TASK_ATTACHMENT_REMOVED','TASK_COMMENTED','TASK_CREATED','TASK_STATUS_CHANGED','TASK_UPDATED') NOT NULL,
  `actor_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_activity_actor` (`actor_id`),
  KEY `idx_activity_type` (`type`),
  KEY `idx_activity_created_at` (`created_at`),
  CONSTRAINT `FKmfjrc8jdvy0yrr7x67qmrxkue` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `board_membership`
--

DROP TABLE IF EXISTS `board_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_membership` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `role` enum('COLLABORATOR','OWNER') NOT NULL,
  `board_id` bigint NOT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_membership_board_user` (`board_id`,`user_id`),
  KEY `idx_board_membership_board_id` (`board_id`),
  KEY `idx_board_membership_user_id` (`user_id`),
  KEY `FKavxk67dlen65bnvfk1rkqk4g` (`created_by_user_id`),
  CONSTRAINT `FK4sylav72mt25s9nbr9cfpjw1s` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKavxk67dlen65bnvfk1rkqk4g` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKkfqk52b2rlrwbnlo5vvoajxvl` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `boards`
--

DROP TABLE IF EXISTS `boards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `boards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `color` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `name` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_board_owner_id` (`owner_id`),
  KEY `idx_board_name` (`name`),
  KEY `idx_board_created_at` (`created_at`),
  KEY `FK32qdrlyxkxq7cm48pviayc3e7` (`project_id`),
  CONSTRAINT `FK32qdrlyxkxq7cm48pviayc3e7` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`),
  CONSTRAINT `FKbng8kmryb5aa0r8p2yq4x2l5l` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_messages`
--

DROP TABLE IF EXISTS `chat_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `encrypted_message` varchar(5000) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `chat_room_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_message_room_timestamp` (`chat_room_id`,`timestamp`),
  KEY `FKgiqeap8ays4lf684x7m0r2729` (`sender_id`),
  CONSTRAINT `FKbcsxusjp1v4rd8879fhvq8ssb` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_rooms` (`id`),
  CONSTRAINT `FKgiqeap8ays4lf684x7m0r2729` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_room_participants`
--

DROP TABLE IF EXISTS `chat_room_participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_room_participants` (
  `chat_room_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`chat_room_id`,`user_id`),
  KEY `FKiqfwuqd8c8i7hrkukc0rii5pg` (`user_id`),
  CONSTRAINT `FKiqfwuqd8c8i7hrkukc0rii5pg` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKkrenof4in9x16he7adf6g058f` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_rooms`
--

DROP TABLE IF EXISTS `chat_rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_room_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `friends`
--

DROP TABLE IF EXISTS `friends`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friends` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `friend_id` bigint NOT NULL,
  `requester_id` bigint NOT NULL,
  `status` enum('ACCEPTED','BLOCKED','PENDING','REJECTED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friend_requester_friend` (`requester_id`,`friend_id`),
  KEY `idx_friend_status` (`status`),
  KEY `idx_friend_created_at` (`created_at`),
  KEY `idx_friend_requester_status` (`requester_id`,`status`),
  KEY `idx_friend_friend_status` (`friend_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jwt_keys`
--

DROP TABLE IF EXISTS `jwt_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jwt_keys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `private_key` longtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_jwt_keys_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `delivered` bit(1) NOT NULL,
  `group_id` bigint DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `message` varchar(500) NOT NULL,
  `type` enum('CHAT_MESSAGE','FRIEND_REQUEST','GROUP','INVITATION','PERSONAL','SYSTEM','TASK_UPDATE') NOT NULL,
  `recipient_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_recipient_created` (`recipient_id`,`created_at`),
  KEY `idx_notification_recipient_read` (`recipient_id`,`is_read`),
  CONSTRAINT `FKqqnsjxlwleyjbxlmm213jaj3f` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_membership`
--

DROP TABLE IF EXISTS `project_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_membership` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `role` enum('COLLABORATOR','OWNER') NOT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_membership_project_user` (`project_id`,`user_id`),
  KEY `idx_project_membership_project_id` (`project_id`),
  KEY `idx_project_membership_user_id` (`user_id`),
  KEY `FKi20dg1hcbv7ygfyry7usab2dm` (`created_by_user_id`),
  CONSTRAINT `FKi20dg1hcbv7ygfyry7usab2dm` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKkbflppuiyrwiu1i4wwwnv9vma` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKrdopdn4cat5b7ho2abxc4qvvg` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `projects`
--

DROP TABLE IF EXISTS `projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `projects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `color` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text NOT NULL,
  `due_date` date NOT NULL,
  `name` varchar(255) NOT NULL,
  `tag` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_project_owner_id` (`owner_id`),
  KEY `idx_project_name` (`name`),
  KEY `idx_project_created_at` (`created_at`),
  CONSTRAINT `FKmueqy6cpcwpfl8gnnag4idjt9` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `refresh_tokens`
--

DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `is_revoked` bit(1) NOT NULL,
  `token` varchar(512) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKghpmfn23vmxfu3spu3lfg4r2d` (`token`),
  KEY `idx_refresh_expiry` (`expiry_date`),
  KEY `idx_refresh_revoked` (`is_revoked`),
  KEY `idx_refresh_user_revoked` (`user_id`,`is_revoked`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_attachments`
--

DROP TABLE IF EXISTS `task_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) NOT NULL,
  `file_url` varchar(2048) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `task_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_attachment_task` (`task_id`),
  CONSTRAINT `FK4eyiisq4wyx2mfj3p9h8ppufo` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_comments`
--

DROP TABLE IF EXISTS `task_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `task_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_comment_task_created` (`task_id`,`created_at`),
  KEY `FK6n4f8xnvwdkbjci078pqdn1w1` (`user_id`),
  CONSTRAINT `FK6n4f8xnvwdkbjci078pqdn1w1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK9517viwn2geh1gpivj6l9y64u` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_labels`
--

DROP TABLE IF EXISTS `task_labels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_labels` (
  `task_id` bigint NOT NULL,
  `label` varchar(100) NOT NULL,
  PRIMARY KEY (`task_id`,`label`),
  CONSTRAINT `FK7wi3dfqb8gx9kiysuy980sbus` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_links`
--

DROP TABLE IF EXISTS `task_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `title` varchar(255) NOT NULL,
  `url` varchar(2048) NOT NULL,
  `task_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_link_task` (`task_id`),
  CONSTRAINT `FK5ep7dbc7yt3kqp06mxe9rb3ol` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_membership`
--

DROP TABLE IF EXISTS `task_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_membership` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `role` enum('COLLABORATOR','OWNER') NOT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `task_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_membership_task_user` (`task_id`,`user_id`),
  KEY `idx_task_membership_task_id` (`task_id`),
  KEY `idx_task_membership_user_id` (`user_id`),
  KEY `FKgaldq5tf3tjnhyijkbsdh5snp` (`created_by_user_id`),
  CONSTRAINT `FKbr39v1e0v237bdjwgxmlxfowx` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `FKgaldq5tf3tjnhyijkbsdh5snp` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKq9i3oiq6gi3as7mqfcyol8p90` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `priority` enum('HIGH','LOW','MID') NOT NULL,
  `status` enum('BLOCKED','COMPLETED','IN_PROGRESS','REVIEW','TODO') NOT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `board_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_status` (`status`),
  KEY `idx_task_created_at` (`created_at`),
  KEY `FKitp79nb81vimv715wd9t8cjmf` (`board_id`),
  KEY `FKh279lo9lqbhxqh68jq9sqs83s` (`owner_id`),
  CONSTRAINT `FKh279lo9lqbhxqh68jq9sqs83s` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKitp79nb81vimv715wd9t8cjmf` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_encryption_keys`
--

DROP TABLE IF EXISTS `user_encryption_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_encryption_keys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `public_key` varchar(512) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4rao12yhg8mfvfkvyu11lw75j` (`user_id`),
  CONSTRAINT `FKfp2n27661ge5a7dldcy3okajm` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `is_online` bit(1) NOT NULL,
  `last_activity` datetime(6) DEFAULT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `onboarding_notified` bit(1) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `profile_picture_url` varchar(2048) DEFAULT NULL,
  `role` enum('ADMIN','COLLABORATOR','OWNER','USER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_email` (`email`),
  KEY `idx_user_name` (`name`),
  KEY `idx_user_last_activity` (`last_activity`),
  KEY `idx_user_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-01 19:11:40
