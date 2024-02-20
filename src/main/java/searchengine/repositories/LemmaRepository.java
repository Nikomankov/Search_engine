package searchengine.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma,Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
}
