DROP TABLE IF EXISTS users CASCADE;
CREATE TABLE  users (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL ,
    google_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(255) DEFAULT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS media_item CASCADE;;
CREATE TABLE  media_item (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    original_image_file_path VARCHAR(255),
    compressed_image_file_path VARCHAR(255),
    thumbnail_file_path VARCHAR(255),
    video_file_path VARCHAR(255),
    content_type VARCHAR(10) NOT NULL,
    media_type VARCHAR(10) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
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

DROP TABLE IF EXISTS media_item_meta_data CASCADE;;
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
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP,
    FOREIGN KEY (media_item_id) REFERENCES media_item(id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS album CASCADE;;
CREATE TABLE  album (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(50) DEFAULT NULL,
    google_id VARCHAR(255) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT 0 on update CURRENT_TIMESTAMP,
    FOREIGN KEY (google_id) REFERENCES users(google_id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS media_item_album CASCADE;;
CREATE TABLE  media_item_album (
    media_item_id INT(11) NOT NULL,
    album_id INT(11) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (media_item_id, album_id),
    FOREIGN KEY (media_item_id) REFERENCES media_item(id),
    FOREIGN KEY (album_id) REFERENCES album(id)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;


