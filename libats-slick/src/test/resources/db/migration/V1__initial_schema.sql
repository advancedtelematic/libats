create table `books` (
  `id` INTEGER NOT NULL PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL,
  `code` VARCHAR(255) NULL
)
;

create table `book_tags` (
  `tag` INTEGER NOT NULL PRIMARY KEY
)
;

create table `book_meta` (
  `book_id` INTEGER NOT NULL,
  `id` INTEGER NOT NULL,
  `tag` INTEGER NOT NULL,
  PRIMARY KEY (`book_id`, `id`),
  CONSTRAINT `book_id_fk`  FOREIGN KEY (`book_id`) REFERENCES `books`(`id`),
  CONSTRAINT `book_tag_fk` FOREIGN KEY (`tag`)     REFERENCES `book_tags`(`tag`)
)
;

create table `re_key_spec`(
  `id` INTEGER NOT NULL PRIMARY KEY,
  `uuid` CHAR(36) NOT NULL UNIQUE,
  `encrypted_col` VARCHAR(255) NOT NULL
);
