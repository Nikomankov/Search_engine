package searchengine.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page,Integer> {
    Optional<Page> findByPath(String path);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Page p WHERE p.`path` = :pth")
    Optional<Page> findByPathAndLock(@Param("pth") String path);

    @Modifying
    @Query("DELETE FROM Page p WHERE p.`site` = :site_id")
    void deleteBySite(@Param("site_id") Site site);

}
