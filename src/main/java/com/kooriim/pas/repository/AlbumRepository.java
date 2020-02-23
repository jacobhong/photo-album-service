package com.kooriim.pas.repository;

import com.kooriim.pas.domain.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Integer> {

  @Modifying
  @Query(value = "INSERT INTO photo_album(album_id, photo_id) VALUES(?1, ?2)", nativeQuery = true)
  void savePhotoAlbum(Integer albumId, Integer photoId);

  List<Album> findByGoogleId(String googleId);
}
