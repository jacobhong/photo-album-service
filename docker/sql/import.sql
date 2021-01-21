CREATE USER 'keycloak131'@'%' IDENTIFIED BY 'password131';
CREATE DATABASE keycloak CHARACTER SET utf8 COLLATE utf8_unicode_ci;
GRANT ALL PRIVILEGES ON *.* TO 'user131'@'%';
GRANT ALL PRIVILEGES ON *.* TO 'keycloak131'@'%';
FLUSH PRIVILEGES;

DROP TABLE IF EXISTS users;
CREATE TABLE  users (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL ,
    google_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS photo;
CREATE TABLE  photo (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    original_image_file_path VARCHAR(255) NOT NULL,
    compressed_image_file_path VARCHAR(255) NOT NULL,
    thumbnail_file_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(10) NOT NULL,
    description VARCHAR(50) DEFAULT NULL,
    google_id VARCHAR(255) NOT NULL,
    is_public TINYINT(1) DEFAULT 0,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP,
    FOREIGN KEY (google_id) REFERENCES users(google_id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS album;
CREATE TABLE  album (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(50) DEFAULT NULL,
    google_id VARCHAR(255) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP,
    FOREIGN KEY (google_id) REFERENCES users(google_id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS photo_album;
CREATE TABLE  photo_album (
    photo_id INT(11) NOT NULL,
    album_id INT(11) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (photo_id, album_id),
    FOREIGN KEY (photo_id) REFERENCES photo(id),
    FOREIGN KEY (album_id) REFERENCES album(id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;



