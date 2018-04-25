create table `books` (
  `id` INTEGER NOT NULL PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL,
  `code` VARCHAR(255) NULL
)
;

create table `re_key_spec`(
  `id` INTEGER NOT NULL PRIMARY KEY,
  `uuid` CHAR(36) NOT NULL UNIQUE,
  `encrypted_col` VARCHAR(255) NOT NULL
);
