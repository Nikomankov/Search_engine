package searchengine.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexM;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.Optional;

@Repository
public interface IndexMRepository extends JpaRepository<IndexM, Integer> {

    Optional<IndexM> findByPageAndLemma(Page page, Lemma lemma);
}
