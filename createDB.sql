SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';


-- -----------------------------------------------------
-- Table `DSS2016_MoodMap`.`keyword`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `DSS2016_MoodMap`.`keyword` ;

CREATE TABLE IF NOT EXISTS `DSS2016_MoodMap`.`keyword` (
  `keyword_id` INT(11) NOT NULL AUTO_INCREMENT,
  `term_eu` VARCHAR(40) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT '',
  `term_es` VARCHAR(40) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT '',
  `term_en` VARCHAR(40) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT '',
  `term_fr` VARCHAR(40) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT '',
  PRIMARY KEY (`keyword_id`))
ENGINE = MyISAM
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;

CREATE UNIQUE INDEX `keyword_id_UNIQUE` ON `DSS2016_MoodMap`.`keyword` (`keyword_id` ASC);


-- -----------------------------------------------------
-- Table `DSS2016_MoodMap`.`user`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `DSS2016_MoodMap`.`user` ;

CREATE TABLE IF NOT EXISTS `DSS2016_MoodMap`.`user` (
  `user_id` INT(11) NOT NULL AUTO_INCREMENT,
  `nickname` VARCHAR(15) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL DEFAULT '',
  `pass` VARCHAR(20) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL DEFAULT '',
  `firstname` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  `surname` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  `email` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  `affiliation` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`))
ENGINE = MyISAM
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;

CREATE UNIQUE INDEX `user_id_UNIQUE` ON `DSS2016_MoodMap`.`user` (`user_id` ASC);

CREATE UNIQUE INDEX `nickname_UNIQUE` ON `DSS2016_MoodMap`.`user` (`nickname` ASC);

CREATE UNIQUE INDEX `pass_UNIQUE` ON `DSS2016_MoodMap`.`user` (`pass` ASC);


-- -----------------------------------------------------
-- Table `DSS2016_MoodMap`.`source`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `DSS2016_MoodMap`.`source` ;

CREATE TABLE IF NOT EXISTS `DSS2016_MoodMap`.`source` (
  `source_id` INT(11) NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(10) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  `influence` FLOAT NULL DEFAULT NULL,
  `source_name` VARCHAR(45) NOT NULL,
  `user_id` INT(11) NULL,
  PRIMARY KEY (`source_id`))
ENGINE = MyISAM
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;

CREATE UNIQUE INDEX `source_id_UNIQUE` ON `DSS2016_MoodMap`.`source` (`source_id` ASC);

CREATE INDEX `fk_source_user1_idx` ON `DSS2016_MoodMap`.`source` (`user_id` ASC);


-- -----------------------------------------------------
-- Table `DSS2016_MoodMap`.`mention`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `DSS2016_MoodMap`.`mention` ;

CREATE TABLE IF NOT EXISTS `DSS2016_MoodMap`.`mention` (
  `mention_id` INT(11) NOT NULL AUTO_INCREMENT,
  `date` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  `source_id` INT(11) NULL DEFAULT NULL,
  `url` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  `text` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  `lang` VARCHAR(5) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  `polarity` VARCHAR(10) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL DEFAULT NULL,
  PRIMARY KEY (`mention_id`))
ENGINE = MyISAM
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;

CREATE INDEX `fk_mention_source1_idx` ON `DSS2016_MoodMap`.`mention` (`source_id` ASC);

CREATE UNIQUE INDEX `mention_id_UNIQUE` ON `DSS2016_MoodMap`.`mention` (`mention_id` ASC);


-- -----------------------------------------------------
-- Table `DSS2016_MoodMap`.`keyword_mention`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `DSS2016_MoodMap`.`keyword_mention` ;

CREATE TABLE IF NOT EXISTS `DSS2016_MoodMap`.`keyword_mention` (
  `mention_id` INT(11) NOT NULL,
  `keyword_id` VARCHAR(40) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  PRIMARY KEY (`mention_id`, `keyword_id`))
ENGINE = MyISAM
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;

CREATE INDEX `fk_keyword_mention_keyword1_idx` ON `DSS2016_MoodMap`.`keyword_mention` (`keyword_id` ASC);


-- -----------------------------------------------------
-- Table `DSS2016_MoodMap`.`user_keyword`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `DSS2016_MoodMap`.`user_keyword` ;

CREATE TABLE IF NOT EXISTS `DSS2016_MoodMap`.`user_keyword` (
  `user_id` VARCHAR(15) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `keyword_id` INT(11) NOT NULL,
  PRIMARY KEY (`user_id`, `keyword_id`))
ENGINE = MyISAM
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_unicode_ci;

CREATE INDEX `fk_user_keyword_keyword1_idx` ON `DSS2016_MoodMap`.`user_keyword` (`keyword_id` ASC);


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
