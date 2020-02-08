package com.webapp.starter.repository;

import com.webapp.starter.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Integer> {

  @Query(value = "SELECT * FROM photo p INNER JOIN photo_album pa ON p.id=pa.photo_id WHERE pa.album_id=?1", nativeQuery = true)
  Optional<List<Photo>> getPhotosByAlbumId(Integer id);

  @Modifying
  @Query(value = "DELETE FROM photo_album where photo_id in (?1)", nativeQuery = true)
  void deletePhotosFromAlbum(List<Integer> photoId);

  List<Photo> getPhotosByGoogleId(String googleId);
//  @Modifying
//  @Query(value = "DELETE FROM photo_album where photo_id in (?1)", nativeQuery = true)
//  void deletePhotosByIds(List<Integer> photoId);
}
