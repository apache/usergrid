CREATE DATABASE  IF NOT EXISTS `instaops_appmanagement` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `instaops_appmanagement`;
-- MySQL dump 10.13  Distrib 5.5.24, for osx10.5 (i386)
--
-- Host: apigeeapmdev.ck0mw2jbysdo.us-east-1.rds.amazonaws.com    Database: instaops_appmanagement
-- ------------------------------------------------------
-- Server version	5.5.27-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `APPLICATIONS`
--

DROP TABLE IF EXISTS `APPLICATIONS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `APPLICATIONS` (
  `instaOpsApplicationId` bigint(20) NOT NULL AUTO_INCREMENT,
  `appName` varchar(255) DEFAULT NULL,
  `appOwner` varchar(255) DEFAULT NULL,
  `applicationUUID` varchar(255) DEFAULT NULL,
  `createdDate` datetime DEFAULT NULL,
  `deleted` bit(1) DEFAULT NULL,
  `environment` varchar(255) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `lastModifiedDate` datetime DEFAULT NULL,
  `monitoringDisabled` bit(1) DEFAULT NULL,
  `orgName` varchar(255) DEFAULT NULL,
  `organizationUUID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`instaOpsApplicationId`),
  UNIQUE KEY `fullAppName` (`fullAppName`),
  KEY `Created_Date` (`createdDate`),
  KEY `OrgAppName` (`fullAppName`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DEVICE_MODEL`
--

DROP TABLE IF EXISTS `DEVICE_MODEL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DEVICE_MODEL` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `appId` bigint(20) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DEVICE_PLATFORM`
--

DROP TABLE IF EXISTS `DEVICE_PLATFORM`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DEVICE_PLATFORM` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `appId` bigint(20) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LOG_CHART_CRITERIA`
--

DROP TABLE IF EXISTS `LOG_CHART_CRITERIA`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LOG_CHART_CRITERIA` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `appConfigType` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `appVersion` varchar(255) DEFAULT NULL,
  `chartName` varchar(255) DEFAULT NULL,
  `defaultChart` bit(1) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOS` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endDate` datetime DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `groupedByApp` bit(1) NOT NULL,
  `groupedByAppConfigType` bit(1) NOT NULL,
  `groupedByAppVersion` bit(1) NOT NULL,
  `groupedByDeviceId` bit(1) NOT NULL,
  `groupedByDeviceModel` bit(1) NOT NULL,
  `groupedByDeviceOS` bit(1) NOT NULL,
  `groupedByDeviceType` bit(1) NOT NULL,
  `groupedByNetworkCarrier` bit(1) NOT NULL,
  `groupedByNetworkCountry` bit(1) NOT NULL,
  `groupedByNetworkType` bit(1) NOT NULL,
  `groupedbyDevicePlatform` bit(1) NOT NULL,
  `lastX` int(11) DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `periodType` int(11) DEFAULT NULL,
  `samplePeriod` int(11) DEFAULT NULL,
  `startDate` datetime DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `userName` varchar(255) DEFAULT NULL,
  `visible` bit(1) NOT NULL,
  `assertCount` bigint(20) DEFAULT NULL,
  `debugCount` bigint(20) DEFAULT NULL,
  `errorAndAboveCount` bigint(20) DEFAULT NULL,
  `errorCount` bigint(20) DEFAULT NULL,
  `eventCount` bigint(20) DEFAULT NULL,
  `infoCount` bigint(20) DEFAULT NULL,
  `showAll` bit(1) NOT NULL,
  `showAssertCount` bit(1) NOT NULL,
  `showDebugCount` bit(1) NOT NULL,
  `showErrorAndAboveCount` bit(1) NOT NULL,
  `showErrorCount` bit(1) NOT NULL,
  `showEvent` bit(1) NOT NULL,
  `showInfoCount` bit(1) NOT NULL,
  `showVerboseCount` bit(1) NOT NULL,
  `showWarnCount` bit(1) NOT NULL,
  `verboseCount` bigint(20) DEFAULT NULL,
  `warnCount` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `LogChartCriteriaByApp` (`appId`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `METRICS_CHART_CRITERIA`
--

DROP TABLE IF EXISTS `METRICS_CHART_CRITERIA`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `METRICS_CHART_CRITERIA` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `appConfigType` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `appVersion` varchar(255) DEFAULT NULL,
  `chartName` varchar(255) DEFAULT NULL,
  `defaultChart` bit(1) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOS` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endDate` datetime DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `groupedByApp` bit(1) NOT NULL,
  `groupedByAppConfigType` bit(1) NOT NULL,
  `groupedByAppVersion` bit(1) NOT NULL,
  `groupedByDeviceId` bit(1) NOT NULL,
  `groupedByDeviceModel` bit(1) NOT NULL,
  `groupedByDeviceOS` bit(1) NOT NULL,
  `groupedByDeviceType` bit(1) NOT NULL,
  `groupedByNetworkCarrier` bit(1) NOT NULL,
  `groupedByNetworkCountry` bit(1) NOT NULL,
  `groupedByNetworkType` bit(1) NOT NULL,
  `groupedbyDevicePlatform` bit(1) NOT NULL,
  `lastX` int(11) DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `periodType` int(11) DEFAULT NULL,
  `samplePeriod` int(11) DEFAULT NULL,
  `startDate` datetime DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `userName` varchar(255) DEFAULT NULL,
  `visible` bit(1) NOT NULL,
  `cacheDataOnly` bit(1) NOT NULL,
  `errored` bit(1) NOT NULL,
  `groupedByDomain` bit(1) NOT NULL,
  `lowerErrorCount` bigint(20) DEFAULT NULL,
  `lowerLatency` bigint(20) DEFAULT NULL,
  `showAll` bit(1) NOT NULL,
  `showError` bit(1) NOT NULL,
  `showLatency` bit(1) NOT NULL,
  `showSamples` bit(1) NOT NULL,
  `upperErrorCount` bigint(20) DEFAULT NULL,
  `upplerLatency` bigint(20) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `NetworkMetricsChartCriteriaByApp` (`appId`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `NETWORK_CARRIERS`
--

DROP TABLE IF EXISTS `NETWORK_CARRIERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `NETWORK_CARRIERS` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `appId` bigint(20) DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `NETWORK_SPEED`
--

DROP TABLE IF EXISTS `NETWORK_SPEED`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `NETWORK_SPEED` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `appId` bigint(20) DEFAULT NULL,
  `networkSpeed` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SESSION_CHART_CRITERIA`
--

DROP TABLE IF EXISTS `SESSION_CHART_CRITERIA`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SESSION_CHART_CRITERIA` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `appConfigType` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `appVersion` varchar(255) DEFAULT NULL,
  `chartName` varchar(255) DEFAULT NULL,
  `defaultChart` bit(1) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOS` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endDate` datetime DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `groupedByApp` bit(1) NOT NULL,
  `groupedByAppConfigType` bit(1) NOT NULL,
  `groupedByAppVersion` bit(1) NOT NULL,
  `groupedByDeviceId` bit(1) NOT NULL,
  `groupedByDeviceModel` bit(1) NOT NULL,
  `groupedByDeviceOS` bit(1) NOT NULL,
  `groupedByDeviceType` bit(1) NOT NULL,
  `groupedByNetworkCarrier` bit(1) NOT NULL,
  `groupedByNetworkCountry` bit(1) NOT NULL,
  `groupedByNetworkType` bit(1) NOT NULL,
  `groupedbyDevicePlatform` bit(1) NOT NULL,
  `lastX` int(11) DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `periodType` int(11) DEFAULT NULL,
  `samplePeriod` int(11) DEFAULT NULL,
  `startDate` datetime DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `userName` varchar(255) DEFAULT NULL,
  `visible` bit(1) NOT NULL,
  `showAll` bit(1) NOT NULL,
  `showSessionCount` bit(1) NOT NULL,
  `showSessionTime` bit(1) NOT NULL,
  `showUsers` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `SessionChartCriteriaByApp` (`appId`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-08-22 20:43:29
