
DROP TABLE IF EXISTS users;
CREATE TABLE IF NOT EXISTS users (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL ,
    google_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS photo;
CREATE TABLE IF NOT EXISTS photo (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    thumbnail_file_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(10) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    google_id VARCHAR(255) NOT NULL,
    is_public TINYINT(1) DEFAULT 0,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    FOREIGN KEY (google_id) REFERENCES users(google_id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS album;
CREATE TABLE IF NOT EXISTS album (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255) DEFAULT NULL,
    google_id VARCHAR(255) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    FOREIGN KEY (google_id) REFERENCES users(google_id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS photo_album;
CREATE TABLE IF NOT EXISTS photo_album (
    photo_id INT(11) NOT NULL,
    album_id INT(11) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (photo_id, album_id),
    FOREIGN KEY (photo_id) REFERENCES photo(id),
    FOREIGN KEY (album_id) REFERENCES album(id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

INSERT INTO `users` (`name`, `google_id`, `email`, `created`, `updated`)
VALUES
	('test', 'anonymousUser', 'anonymousUser', '2020-02-23 02:48:35', '2020-02-23 02:48:35');
--INSERT INTO `photo` (`id`, `title`, `file_path`, `thumbnail_file_path`, `content_type`, `description`, `google_id`, `created`, `updated`)
--VALUES
--	(1, 'test', 'test', 'test', 'test', 'test', 'anonymousUser', '2020-02-23 02:48:35', '2020-02-23 02:48:35');
--INSERT INTO `photo` (`id`, `title`, `file_path`, `thumbnail_file_path`, `content_type`, `description`, `google_id`, `created`, `updated`)
--VALUES
--	(2, 'test', 'test', 'test', 'test', 'test', 'anonymousUser', '2020-02-23 02:48:35', '2020-02-23 02:48:35');
