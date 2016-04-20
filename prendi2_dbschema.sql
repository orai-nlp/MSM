SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

DROP SCHEMA IF EXISTS `prendi2_db` ;
CREATE SCHEMA IF NOT EXISTS `prendi2_db` DEFAULT CHARACTER SET utf8mb4 ;
USE `prendi2_db` ;

-- -----------------------------------------------------
-- Table `prendi2_db`.`behagunea_app_keyword`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `prendi2_db`.`behagunea_app_keyword` ;

CREATE TABLE IF NOT EXISTS `prendi2_db`.`behagunea_app_keyword` (
  `keyword_id` INT(11) NOT NULL,
  `type` INT(5) NOT NULL,
  `lang` VARCHAR(5) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `category` VARCHAR(40) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `term` LONGTEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `screen_tag` LONGTEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  PRIMARY KEY (`keyword_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;


-- -----------------------------------------------------
-- Table `prendi2_db`.`behagunea_app_source`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `prendi2_db`.`behagunea_app_source` ;

CREATE TABLE IF NOT EXISTS `prendi2_db`.`behagunea_app_source` (
  `source_id` BIGINT(20) NOT NULL DEFAULT '0',
  `type` VARCHAR(10) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `influence` DOUBLE NULL DEFAULT NULL,
  `source_name` VARCHAR(45) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `last_fetch` LONGTEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `user_id` INT(11) NULL DEFAULT NULL,
  `domain` LONGTEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `geoinfo` VARCHAR(80) NULL,
  `followers` INT(11) NULL,
  `friends` INT(11) NULL,
  PRIMARY KEY (`source_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;


-- -----------------------------------------------------
-- Table `prendi2_db`.`behagunea_app_cluster`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `prendi2_db`.`behagunea_app_cluster` ;

CREATE TABLE IF NOT EXISTS `prendi2_db`.`behagunea_app_cluster` (
  `cluster_id` INT(11) NOT NULL,
  `name` VARCHAR(45) NULL,
  PRIMARY KEY (`cluster_id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `prendi2_db`.`behagunea_app_mention`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `prendi2_db`.`behagunea_app_mention` ;

CREATE TABLE IF NOT EXISTS `prendi2_db`.`behagunea_app_mention` (
  `mention_id` INT(11) NOT NULL,
  `date` DATETIME NOT NULL,
  `url` LONGTEXT CHARACTER SET 'utf8mb4' NOT NULL,
  `text` LONGTEXT CHARACTER SET 'utf8mb4' NOT NULL,
  `lang` VARCHAR(5) CHARACTER SET 'utf8mb4' NOT NULL,
  `polarity` VARCHAR(10) CHARACTER SET 'utf8mb4' NOT NULL,
  `favourites` INT(11) NOT NULL,
  `retweets` INT(11) NOT NULL,
  `source_id` BIGINT(20) NULL DEFAULT NULL,
  `corrected` TINYINT(1) NOT NULL DEFAULT 0,
  `manual_polarity` VARCHAR(10) CHARACTER SET 'utf8mb4' NULL,
  `geoinfo` VARCHAR(80) NULL,
  `cluster_id` INT NULL,
  `native_id` BIGINT(30) NULL,
  `retweet_id` BIGINT(30) NULL,
  PRIMARY KEY (`mention_id`),
  INDEX `behagunea_app_mention_0afd9202` (`source_id` ASC),
  INDEX `fk_behagunea_app_mention_cluster1_idx` (`cluster_id` ASC),
  UNIQUE INDEX `orig_tweet_id_UNIQUE` (`native_id` ASC),
  CONSTRAINT `behagunea_app_mention_source_id_40597b0b4f5263a7_fk`
    FOREIGN KEY (`source_id`)
    REFERENCES `prendi2_db`.`behagunea_app_source` (`source_id`),
  CONSTRAINT `fk_behagunea_app_mention_cluster1`
    FOREIGN KEY (`cluster_id`)
    REFERENCES `prendi2_db`.`behagunea_app_cluster` (`cluster_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4;


-- -----------------------------------------------------
-- Table `prendi2_db`.`behagunea_app_keyword_mention`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `prendi2_db`.`behagunea_app_keyword_mention` ;

CREATE TABLE IF NOT EXISTS `prendi2_db`.`behagunea_app_keyword_mention` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `keyword_id` INT(11) NOT NULL,
  `mention_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `behagune_keyword_id_e1d999f2_fk_behagunea_app_keyword_keyword_id` (`keyword_id` ASC),
  INDEX `behagunea_app_keyword_mention_31e0dc7e` (`mention_id` ASC),
  CONSTRAINT `behagune_keyword_id_e1d999f2_fk_behagunea_app_keyword_keyword_id`
    FOREIGN KEY (`keyword_id`)
    REFERENCES `prendi2_db`.`behagunea_app_keyword` (`keyword_id`),
  CONSTRAINT `mention_id_4844d90a89de1133_fk_behagunea_app_mention_mention_id`
    FOREIGN KEY (`mention_id`)
    REFERENCES `prendi2_db`.`behagunea_app_mention` (`mention_id`))
ENGINE = InnoDB
AUTO_INCREMENT = 91456
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;


-- -----------------------------------------------------
-- Table `prendi2_db`.`behagunea_app_cluster_keyword`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `prendi2_db`.`behagunea_app_cluster_keyword` ;

CREATE TABLE IF NOT EXISTS `prendi2_db`.`behagunea_app_cluster_keyword` (
  `cluster_id` INT(11) NOT NULL,
  `keyword_id` INT(11) NOT NULL,
  PRIMARY KEY (`cluster_id`, `keyword_id`),
  INDEX `fk_cluster_has_behagunea_app_keyword_behagunea_app_keyword1_idx` (`keyword_id` ASC),
  INDEX `fk_cluster_has_behagunea_app_keyword_cluster1_idx` (`cluster_id` ASC),
  CONSTRAINT `fk_cluster_has_behagunea_app_keyword_cluster1`
    FOREIGN KEY (`cluster_id`)
    REFERENCES `prendi2_db`.`behagunea_app_cluster` (`cluster_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_cluster_has_behagunea_app_keyword_behagunea_app_keyword1`
    FOREIGN KEY (`keyword_id`)
    REFERENCES `prendi2_db`.`behagunea_app_keyword` (`keyword_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE USER 'prendi2' IDENTIFIED BY 'prendi$Admin';
GRANT USAGE ON *.* TO prendi2;
GRANT ALL ON `prendi2_db`.* TO 'prendi2';
