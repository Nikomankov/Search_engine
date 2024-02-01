package searchengine.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.Date;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Site s WHERE s.id = :id")
    Optional<Site> findAndLock(@Param("id") int id);
}
