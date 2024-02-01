package searchengine.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import org.hibernate.annotations.DynamicUpdate;


@Data
@Builder
@DynamicUpdate
@Entity
@Table(name = "`index`")
public class IndexM {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "`rank`", columnDefinition = "float", nullable = false)
    private float rank;

    @Tolerate
    public IndexM(){}

    @Override
    public boolean equals(Object obj){
        IndexM index = (IndexM) obj;
        return (index.getPage().equals(this.page) && index.getLemma().equals(this.lemma));
    }

    @Override
    public int hashCode(){
        int total = 31;
        total = total * 31 + id;
        total = total * 31 + (page == null ? 0 : page.hashCode());
        total = total * 31 + (lemma == null ? 0 : lemma.hashCode());
        return total;
    }

    @Override
    public String toString(){
        return new StringBuilder("\nLemma")
                .append("\n\tID --- ").append(id)
                .append("\n\tPage - ").append(page.getPath())
                .append("\n\tLemma - ").append(lemma)
                .append("\n\tRank - ").append(rank)
                .toString();
    }
}
