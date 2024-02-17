package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.sql.Date;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);

    @Modifying
    @Query(value = "UPDATE Site s SET s.status_time = ?2 WHERE s.id = ?1", nativeQuery = true)
    void updateDate(int id, Date date);

    @Modifying
    @Query(value = "UPDATE Site s SET s.status_time = ?2, s.status = ?3 WHERE s.id = ?1", nativeQuery = true)
    void updateDateAndStatus(int id, Date date, String status);
}
