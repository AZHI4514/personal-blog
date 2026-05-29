-- MySQL dump 10.13  Distrib 9.6.0, for Win64 (x86_64)
--
-- Host: localhost    Database: blog_db
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `blog_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `blog_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `blog_db`;

--
-- Table structure for table `clap`
--

DROP TABLE IF EXISTS `clap`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `clap` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `total` bigint NOT NULL DEFAULT '0' COMMENT '累计拍手总数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拍手表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `clap`
--

LOCK TABLES `clap` WRITE;
/*!40000 ALTER TABLE `clap` DISABLE KEYS */;
INSERT INTO `clap` VALUES (1,6);
/*!40000 ALTER TABLE `clap` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `image`
--

DROP TABLE IF EXISTS `image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储路径',
  `author` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作者名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `image`
--

LOCK TABLES `image` WRITE;
/*!40000 ALTER TABLE `image` DISABLE KEYS */;
INSERT INTO `image` VALUES (3,'/uploads/images/image3.png','unknown','2026-04-10 21:03:40'),(4,'/uploads/images/image4.png','unknown','2026-04-12 17:16:02'),(5,'/uploads/images/image5.jpg','unknown','2026-04-12 17:21:55'),(7,'/uploads/images/image7.jpg','unknown','2026-04-12 17:21:59'),(8,'/uploads/images/image8.jpg','unknown','2026-04-12 17:21:58'),(10,'/uploads/images/image10.jpg','unknown','2026-04-12 17:22:01'),(11,'/uploads/images/image11.jpg','unknown','2026-04-12 17:22:02'),(12,'/uploads/images/image12.jpg','unknown','2026-04-12 17:22:03');
/*!40000 ALTER TABLE `image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `music`
--

DROP TABLE IF EXISTS `music`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `music` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '曲名',
  `artist` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '艺术家',
  `file_path` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '音频文件路径',
  `cover_path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片路径',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='音乐表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `music`
--

LOCK TABLES `music` WRITE;
/*!40000 ALTER TABLE `music` DISABLE KEYS */;
INSERT INTO `music` VALUES (1,'夜空','鈴木みのり','/uploads/musics/夜空.mp3','/uploads/images/夜空.jpg','2026-05-04 15:35:13'),(2,'だんご大家族','茶太 (ちゃた)','/uploads/musics/だんご大家族.mp3','/uploads/images/だんご大家族.jpg','2026-05-04 15:25:35'),(3,'青い栞','Galileo Galilei','/uploads/musics/青い栞.mp3','/uploads/images/青い栞.jpg','2026-05-04 15:25:35'),(4,'secret base ～君がくれたもの～(10 years after Ver.) ','本間芽衣子 (CV.茅野愛衣), 安城鳴子 (CV.戸松遥), 鶴見知利子 (CV.早見沙織)','/uploads/musics/secret base ～君がくれたもの～(10 years after Ver.) .mp3','/uploads/images/secret base ～君がくれたもの～(10 years after Ver.).jpg','2026-05-04 15:34:16'),(5,'メグメル','riya','/uploads/musics/メグメル.mp3','/uploads/images/メグメル.jpg','2026-05-04 15:34:16'),(6,'Hectopascal','小糸侑 (CV.高田憂希), 七海燈子 (CV.寿美菜子)','/uploads/musics/Hectopascal.mp3','/uploads/images/Hectopascal.jpg','2026-05-04 15:34:16'),(7,'みちしるべ','茅原実里','/uploads/musics/みちしるべ.mp3','/uploads/images/みちしるべ路标.jpg','2026-05-04 15:34:16'),(8,'Sincerely','TRUE (唐沢美帆)','/uploads/musics/Sincerely.mp3','/uploads/images/Sincerely.jpg','2026-05-04 15:34:16'),(9,'U&I','放課後ティータイム','/uploads/musics/U&I.mp3','/uploads/images/U&I.jpg','2026-05-04 15:34:16'),(10,'天使にふれたよ','放課後ティータイム','/uploads/musics/天使にふれたよ.mp3','/uploads/images/天使にふれたよ.jpg','2026-05-04 15:34:16'),(11,'ふわふわ时间','桜高軽音部','/uploads/musics/ふわふわ时间.mp3','/uploads/images/ふわふわ时间.jpg','2026-05-04 15:34:16'),(12,'reply','輝夜 (CV.夏吉優子)','/uploads/musics/reply.mp3','/uploads/images/ray（超かぐや姫）.jpg','2026-05-04 15:34:16'),(13,'おがえり','小岩井ことり','/uploads/musics/おがえり.mp3','/uploads/images/おがえり.jpg','2026-05-04 15:34:16'),(14,'君に会えた日与你相遇之日','鬼頭明里, 伊藤美来','/uploads/musics/君に会えた日与你相遇之日.mp3','/uploads/images/君に会えた日与你相遇之日.jpg','2026-05-04 15:34:16'),(15,'星降る海','月見八千代 (CV.早見沙織)','/uploads/musics/星降る海.mp3','/uploads/images/ray（超かぐや姫）.jpg','2026-05-04 15:34:16'),(16,'そんなもんね就是这样啊','asmi','/uploads/musics/そんなもんね就是这样啊.mp3','/uploads/images/そんなもんね就是这样啊.jpg','2026-05-04 15:34:16'),(17,'味噌汁とバター味增汤和黄油','汐れいら','/uploads/musics/味噌汁とバター味增汤和黄油.mp3','/uploads/images/味噌汁とバター味增汤和黄油.jpg','2026-05-04 15:34:16'),(18,'私は、わたしの事が好き','輝夜 (CV.夏吉優子)','/uploads/musics/私は、わたしの事が好き.mp3','/uploads/images/ray（超かぐや姫）.jpg','2026-05-04 15:34:16'),(19,'Remember','月見ヤチヨ (cv.早見沙織)','/uploads/musics/Remember.mp3','/uploads/images/remember.jpg','2026-05-04 15:34:16'),(20,'ray','TRUE (唐沢美帆)','/uploads/musics/ray.mp3','/uploads/images/ray（超かぐや姫）.jpg','2026-05-04 15:34:16'),(21,'打上花火','DAOKO, 米津玄師','/uploads/musics/打上花火.mp3','/uploads/images/打上花火.jpg','2026-05-04 15:34:16'),(22,'転がる岩、君に朝が降る','結束バンド','/uploads/musics/転がる岩、君に朝が降る.mp3','/uploads/images/転がる岩、君に朝が降る.jpg','2026-05-04 15:34:16'),(23,'星座になれたら','結束バンド','/uploads/musics/星座になれたら.mp3','/uploads/images/星座になれたら.jpg','2026-05-04 15:34:16'),(24,'忘れてやらない','結束バンド','/uploads/musics/忘れてやらない.mp3','/uploads/images/忘れてやらない.jpg','2026-05-04 15:34:16'),(25,'ギターと孤独と蒼い惑星','結束バンド','/uploads/musics/ギターと孤独と蒼い惑星.mp3','/uploads/images/ギターと孤独と蒼い惑星.jpg','2026-05-04 15:34:16'),(26,'なにが悪い','結束バンド','/uploads/musics/なにが悪い.mp3','/uploads/images/なにが悪い.jpg','2026-05-04 15:34:16'),(27,'カラカラ','結束バンド','/uploads/musics/カラカラ.mp3','/uploads/images/カラカラ.jpg','2026-05-04 15:34:16'),(28,'Distortion!!','結束バンド','/uploads/musics/Distortion!!.mp3','/uploads/images/Distortion!!.jpg','2026-05-04 15:34:16');
/*!40000 ALTER TABLE `music` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post`
--

DROP TABLE IF EXISTS `post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint DEFAULT NULL COMMENT '父帖子ID，空为顶级帖子，非空为回复',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发布者名字',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发布者邮箱',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '帖子标题（回复可选）',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `image_path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件图片路径',
  `delete_key` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '删除验证密钥',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post`
--

LOCK TABLES `post` WRITE;
/*!40000 ALTER TABLE `post` DISABLE KEYS */;
INSERT INTO `post` VALUES (7,NULL,'AZHI4514','azhi4514@azhiblog.com','第一次连接数据库测试','测试文本...','','test','2026-05-05 10:42:50','2026-05-05 10:42:50'),(8,7,'AZHI4514','azhi4514@azhiblog.com','回复测试1','回复...回复...上传文件测试...','/uploads/images/331f2807-8be5-49bb-952c-e9ef874381ac.png','test','2026-05-05 10:44:43','2026-05-05 10:44:43');
/*!40000 ALTER TABLE `post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `visitor_stat`
--

DROP TABLE IF EXISTS `visitor_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `visitor_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `total` bigint NOT NULL DEFAULT '0' COMMENT '累计访客总数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访客统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visitor_stat`
--

LOCK TABLES `visitor_stat` WRITE;
/*!40000 ALTER TABLE `visitor_stat` DISABLE KEYS */;
INSERT INTO `visitor_stat` VALUES (1,28);
/*!40000 ALTER TABLE `visitor_stat` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-05 13:19:05
