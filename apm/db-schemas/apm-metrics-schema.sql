CREATE DATABASE  IF NOT EXISTS `instaops_analytics` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `instaops_analytics`;
-- MySQL dump 10.13  Distrib 5.5.24, for osx10.5 (i386)
--
-- Host: apigeeapmdev.ck0mw2jbysdo.us-east-1.rds.amazonaws.com    Database: instaops_analytics
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
-- Table structure for table `CLIENT_LOG`
--

DROP TABLE IF EXISTS `CLIENT_LOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CLIENT_LOG` (
  `id` bigint(20) NOT NULL,
  `CONFIG_TYPE` varchar(255) NOT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `applicationVersion` varchar(255) DEFAULT NULL,
  `bearing` float DEFAULT NULL,
  `correctedTimestamp` datetime DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOSVersion` varchar(255) DEFAULT NULL,
  `deviceOperatingSystem` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endDay` bigint(20) DEFAULT NULL,
  `endHour` bigint(20) DEFAULT NULL,
  `endMinute` bigint(20) DEFAULT NULL,
  `endMonth` bigint(20) DEFAULT NULL,
  `endWeek` bigint(20) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `isNetworkRoaming` bit(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `logLevel` varchar(255) DEFAULT NULL,
  `logMessage` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `sessionId` varchar(255) DEFAULT NULL,
  `tag` varchar(255) DEFAULT NULL,
  `timeStamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `appIdEndMinute` (`appId`,`endMinute`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CLIENT_NETWORK_METRICS`
--

DROP TABLE IF EXISTS `CLIENT_NETWORK_METRICS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CLIENT_NETWORK_METRICS` (
  `id` bigint(20) NOT NULL,
  `appConfigType` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `applicationVersion` varchar(255) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOSVersion` varchar(255) DEFAULT NULL,
  `deviceOperatingSystem` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `domain` varchar(255) DEFAULT NULL,
  `endDay` bigint(20) DEFAULT NULL,
  `endHour` bigint(20) DEFAULT NULL,
  `endMinute` bigint(20) DEFAULT NULL,
  `endMonth` bigint(20) DEFAULT NULL,
  `endTime` datetime DEFAULT NULL,
  `endWeek` bigint(20) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `httpStatusCode` bigint(20) DEFAULT NULL,
  `isNetworkRoaming` bit(1) DEFAULT NULL,
  `latency` bigint(20) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `numErrors` bigint(20) DEFAULT NULL,
  `numSamples` bigint(20) DEFAULT NULL,
  `responseDataSize` bigint(20) DEFAULT NULL,
  `serverId` varchar(255) DEFAULT NULL,
  `serverProcessingTime` bigint(20) DEFAULT NULL,
  `serverReceiptTime` datetime DEFAULT NULL,
  `serverResponseTime` datetime DEFAULT NULL,
  `sessionId` varchar(255) DEFAULT NULL,
  `startTime` datetime DEFAULT NULL,
  `timeStamp` datetime DEFAULT NULL,
  `transactionDetails` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `appIdEndMinute` (`appId`,`endMinute`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CLIENT_SESSION_METRICS`
--

DROP TABLE IF EXISTS `CLIENT_SESSION_METRICS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CLIENT_SESSION_METRICS` (
  `id` bigint(20) NOT NULL,
  `appConfigType` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `applicationVersion` varchar(255) DEFAULT NULL,
  `batteryLevel` int(11) DEFAULT NULL,
  `bearing` float DEFAULT NULL,
  `deviceCountry` varchar(255) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOSVersion` varchar(255) DEFAULT NULL,
  `deviceOperatingSystem` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endDay` bigint(20) DEFAULT NULL,
  `endHour` bigint(20) DEFAULT NULL,
  `endMinute` bigint(20) DEFAULT NULL,
  `endMonth` bigint(20) DEFAULT NULL,
  `endWeek` bigint(20) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `isNetworkChanged` bit(1) DEFAULT NULL,
  `isNetworkRoaming` bit(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `localCountry` varchar(255) DEFAULT NULL,
  `localLanguage` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkExtraInfo` varchar(255) DEFAULT NULL,
  `networkSubType` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `networkTypeName` varchar(255) DEFAULT NULL,
  `sdkType` varchar(255) DEFAULT NULL,
  `sdkVersion` varchar(255) DEFAULT NULL,
  `sessionId` varchar(255) DEFAULT NULL,
  `sessionStartTime` datetime DEFAULT NULL,
  `telephonyDeviceId` varchar(255) DEFAULT NULL,
  `telephonyNetworkOperator` varchar(255) DEFAULT NULL,
  `telephonyNetworkOperatorName` varchar(255) DEFAULT NULL,
  `telephonyNetworkType` varchar(255) DEFAULT NULL,
  `telephonyPhoneType` varchar(255) DEFAULT NULL,
  `telephonySignalStrength` varchar(255) DEFAULT NULL,
  `timeStamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `appIdEndMinute` (`appId`,`endMinute`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `COMPACT_CLIENT_LOG`
--

DROP TABLE IF EXISTS `COMPACT_CLIENT_LOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `COMPACT_CLIENT_LOG` (
  `id` bigint(20) NOT NULL,
  `appConfigType` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `applicationVersion` varchar(255) DEFAULT NULL,
  `assertCount` bigint(20) DEFAULT NULL,
  `bearing` float DEFAULT NULL,
  `chartCriteria` varchar(255) DEFAULT NULL,
  `chartCriteriaId` bigint(20) DEFAULT NULL,
  `crashCount` bigint(20) DEFAULT NULL,
  `debugCount` bigint(20) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOperatingSystem` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endDay` bigint(20) DEFAULT NULL,
  `endHour` bigint(20) DEFAULT NULL,
  `endMinute` bigint(20) DEFAULT NULL,
  `endMonth` bigint(20) DEFAULT NULL,
  `endWeek` bigint(20) DEFAULT NULL,
  `endYear` bigint(20) DEFAULT NULL,
  `errorAndAboveCount` bigint(20) DEFAULT NULL,
  `errorCount` bigint(20) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `infoCount` bigint(20) DEFAULT NULL,
  `isNetworkRoaming` bit(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `logLevel` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `SAMPLE_PERIOD` varchar(255) NOT NULL,
  `timeStamp` datetime DEFAULT NULL,
  `verboseCount` bigint(20) DEFAULT NULL,
  `warnCount` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `LogChartHour` (`appId`,`chartCriteriaId`,`endHour`),
  KEY `LogChartMinute` (`appId`,`chartCriteriaId`,`endMinute`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `COMPACT_NETWORK_METRICS`
--

DROP TABLE IF EXISTS `COMPACT_NETWORK_METRICS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `COMPACT_NETWORK_METRICS` (
  `id` bigint(20) NOT NULL,
  `appConfigType` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `applicationVersion` varchar(255) DEFAULT NULL,
  `chartCriteria` varchar(255) DEFAULT NULL,
  `chartCriteriaId` bigint(20) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOperatingSystem` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `domain` varchar(255) DEFAULT NULL,
  `endDay` bigint(20) DEFAULT NULL,
  `endHour` bigint(20) DEFAULT NULL,
  `endMinute` bigint(20) DEFAULT NULL,
  `endMonth` bigint(20) DEFAULT NULL,
  `endWeek` bigint(20) DEFAULT NULL,
  `endYear` bigint(20) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `groupedByApp` bit(1) NOT NULL,
  `groupedByNetworkType` bit(1) NOT NULL,
  `groupedByNeworkProvider` bit(1) NOT NULL,
  `isNetworkRoaming` bit(1) DEFAULT NULL,
  `maxLatency` bigint(20) DEFAULT NULL,
  `maxServerLatency` bigint(20) DEFAULT NULL,
  `minLatency` bigint(20) DEFAULT NULL,
  `minServerLatency` bigint(20) DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `numErrors` bigint(20) DEFAULT NULL,
  `numSamples` bigint(20) DEFAULT NULL,
  `SAMPLE_PERIOD` varchar(255) NOT NULL,
  `sumLatency` bigint(20) DEFAULT NULL,
  `sumServerLatency` bigint(20) DEFAULT NULL,
  `timeStamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `NetworkMetricsChartMinute` (`appId`,`chartCriteriaId`,`endMinute`),
  KEY `NetworkMetricsChartHour` (`appId`,`chartCriteriaId`,`endHour`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `COMPACT_SESSION_METRICS`
--

DROP TABLE IF EXISTS `COMPACT_SESSION_METRICS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `COMPACT_SESSION_METRICS` (
  `id` bigint(20) NOT NULL,
  `CONFIG_TYPE` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `applicationVersion` varchar(255) DEFAULT NULL,
  `chartCriteria` varchar(255) DEFAULT NULL,
  `chartCriteriaId` bigint(20) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOperatingSystem` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endBatteryLevel` int(11) DEFAULT NULL,
  `endDay` bigint(20) DEFAULT NULL,
  `endHour` bigint(20) DEFAULT NULL,
  `endMinute` bigint(20) DEFAULT NULL,
  `endMonth` bigint(20) DEFAULT NULL,
  `endWeek` bigint(20) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `isNetworkChanged` bit(1) DEFAULT NULL,
  `isNetworkRoaming` bit(1) DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `numUniqueUsers` bigint(20) DEFAULT NULL,
  `SAMPLE_PERIOD` varchar(255) NOT NULL,
  `sessionCount` bigint(20) DEFAULT NULL,
  `startBatteryLevel` int(11) DEFAULT NULL,
  `sumBatteryConsumption` bigint(20) DEFAULT NULL,
  `sumSessionLength` bigint(20) DEFAULT NULL,
  `sumSessionUserActivity` bigint(20) DEFAULT NULL,
  `timeStamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `SessionMetricsChartMinute` (`appId`,`chartCriteriaId`,`endMinute`),
  KEY `SessionMetricsChartHour` (`appId`,`chartCriteriaId`,`endHour`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CRASH_LOG_DETAILS`
--

DROP TABLE IF EXISTS `CRASH_LOG_DETAILS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CRASH_LOG_DETAILS` (
  `id` bigint(20) NOT NULL,
  `appConfigType` varchar(255) DEFAULT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `applicationVersion` varchar(255) DEFAULT NULL,
  `bearing` float DEFAULT NULL,
  `crashFileName` varchar(255) DEFAULT NULL,
  `crashSummary` varchar(255) DEFAULT NULL,
  `customMetaData` varchar(255) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOperatingSystem` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endDay` bigint(20) DEFAULT NULL,
  `endHour` bigint(20) DEFAULT NULL,
  `endMinute` bigint(20) DEFAULT NULL,
  `endMonth` bigint(20) DEFAULT NULL,
  `endWeek` bigint(20) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `isNetworkRoaming` bit(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `timeStamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `appIdEndMinute` (`appId`,`endMinute`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SUMMARY_SESSION_METRICS`
--

DROP TABLE IF EXISTS `SUMMARY_SESSION_METRICS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SUMMARY_SESSION_METRICS` (
  `id` bigint(20) NOT NULL,
  `CONFIG_TYPE` varchar(255) NOT NULL,
  `appId` bigint(20) DEFAULT NULL,
  `applicationVersion` varchar(255) DEFAULT NULL,
  `batteryConsumption` int(11) DEFAULT NULL,
  `chartCriteriaId` bigint(20) DEFAULT NULL,
  `deviceCountry` varchar(255) DEFAULT NULL,
  `deviceId` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `deviceOperatingSystem` varchar(255) DEFAULT NULL,
  `devicePlatform` varchar(255) DEFAULT NULL,
  `deviceType` varchar(255) DEFAULT NULL,
  `endBatteryLevel` int(11) DEFAULT NULL,
  `endDay` bigint(20) DEFAULT NULL,
  `endHour` bigint(20) DEFAULT NULL,
  `endMinute` bigint(20) DEFAULT NULL,
  `endMonth` bigint(20) DEFAULT NULL,
  `endWeek` bigint(20) DEFAULT NULL,
  `fullAppName` varchar(255) DEFAULT NULL,
  `isActiveSession` bit(1) DEFAULT NULL,
  `localCountry` varchar(255) DEFAULT NULL,
  `localLanguage` varchar(255) DEFAULT NULL,
  `networkCarrier` varchar(255) DEFAULT NULL,
  `networkCountry` varchar(255) DEFAULT NULL,
  `networkType` varchar(255) DEFAULT NULL,
  `sessionEndTime` datetime DEFAULT NULL,
  `sessionExpiryTime` datetime DEFAULT NULL,
  `sessionExplicitlyEnded` bit(1) DEFAULT NULL,
  `sessionId` varchar(255) DEFAULT NULL,
  `sessionLength` bigint(20) NOT NULL,
  `sessionStartTime` datetime DEFAULT NULL,
  `startBatteryLevel` int(11) DEFAULT NULL,
  `startDay` bigint(20) DEFAULT NULL,
  `startHour` bigint(20) DEFAULT NULL,
  `startMinute` bigint(20) DEFAULT NULL,
  `startMonth` bigint(20) DEFAULT NULL,
  `startWeek` bigint(20) DEFAULT NULL,
  `userActivityCount` bigint(20) DEFAULT NULL,
  `userActivityLength` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `SummarySessionDevicesForCEP` (`appId`,`sessionId`),
  KEY `SummarySessionForMinuteChart` (`appId`,`endMinute`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hibernate_sequences`
--

DROP TABLE IF EXISTS `hibernate_sequences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hibernate_sequences` (
  `sequence_name` varchar(255) DEFAULT NULL,
  `sequence_next_hi_value` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-08-22 20:42:47
