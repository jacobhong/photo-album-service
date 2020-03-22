package com.kooriim.pas.repository;

import com.kooriim.pas.domain.Photo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Integer> {

  @Query(value = "SELECT * FROM photo p INNER JOIN photo_album pa ON p.id=pa.photo_id WHERE pa.album_id=?1", nativeQuery = true)
  Optional<List<Photo>> getPhotosByAlbumId(Integer id, Pageable pageable);

  Optional<List<Photo>> findByIsPublicTrue();

  @Modifying
  @Query(value = "DELETE FROM photo_album where photo_id in (?1)", nativeQuery = true)
  void deletePhotosFromAlbum(List<Integer> photoId);

  @Modifying
  @Query(value = "DELETE FROM photo_album where album_id = ?1", nativeQuery = true)
  void deleteAllPhotosByAlbumId(Integer albumId);

  Optional<List<Photo>> getPhotosByGoogleId(String googleId, Pageable pageable);
}
