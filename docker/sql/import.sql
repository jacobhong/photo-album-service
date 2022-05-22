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
    refresh_token VARCHAR(255) DEFAULT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS media_item;
CREATE TABLE  media_item (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    original_image_file_path VARCHAR(500),
    compressed_image_file_path VARCHAR(500),
    thumbnail_file_path VARCHAR(500),
    video_file_path VARCHAR(500),
    content_type VARCHAR(10) NOT NULL,
    media_type VARCHAR(10) NOT NULL,
    description VARCHAR(50) DEFAULT NULL,
    google_id VARCHAR(255) NOT NULL,
    is_public TINYINT(1) DEFAULT 0,
    thumbnail_image_file_size INT(11) DEFAULT NULL,
    compressed_image_file_size INT(11) DEFAULT NULL,
    original_image_file_size INT(11) DEFAULT NULL,
    video_file_size INT(11) DEFAULT NULL,
    original_date TIMESTAMP DEFAULT 0,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP,
    FOREIGN KEY (google_id) REFERENCES users(google_id),
    CONSTRAINT title_google_id UNIQUE(title, google_id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS media_item_meta_data;
CREATE TABLE  media_item_meta_data (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    media_item_id INT(11) NOT NULL,
    camera_make VARCHAR(50) DEFAULT NULL,
    camera_model VARCHAR(50) DEFAULT NULL,
    focal_length SMALLINT(10) DEFAULT NULL,
    aperture_f_number SMALLINT(10) DEFAULT NULL,
    iso_equivalent SMALLINT(10) DEFAULT NULL,
    exposure_time SMALLINT(10) DEFAULT NULL,
    width SMALLINT(10) DEFAULT NULL,
    height SMALLINT(10) DEFAULT NULL,
    fps SMALLINT(5) DEFAULT NULL,

    contrast VARCHAR(50) DEFAULT NULL,
    digital_zoom_ratio SMALLINT(10) DEFAULT NULL,
    exposure_compensation SMALLINT(10) DEFAULT NULL,
    exposure_mode VARCHAR(50) DEFAULT NULL,
    exposure_program VARCHAR(50) DEFAULT NULL,
    lens_model VARCHAR(50) DEFAULT NULL,
    metering_mode VARCHAR(50) DEFAULT NULL,
    saturation VARCHAR(50) DEFAULT NULL,
    scene_capture_type VARCHAR(50) DEFAULT NULL,
    sharpness VARCHAR(50) DEFAULT NULL,
    white_balance VARCHAR(50) DEFAULT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP,
    FOREIGN KEY (media_item_id) REFERENCES media_item(id)
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

DROP TABLE IF EXISTS media_item_album;
CREATE TABLE  media_item_album (
    media_item_id INT(11) NOT NULL,
    album_id INT(11) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (media_item_id, album_id),
    FOREIGN KEY (media_item_id) REFERENCES media_item(id),
    FOREIGN KEY (album_id) REFERENCES album(id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

