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
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @Tolerate
    public Lemma(){}

    @Override
    public boolean equals(Object obj){
        Lemma lemma = (Lemma) obj;
        return (lemma.getLemma().equals(this.lemma) && lemma.site.equals(this.site));
    }

    public void increaseFrequency(){
        this.frequency = this.frequency + 1;
    }

    @Override
    public int hashCode(){
        int total = 31;
        total = total * 31 + id;
        total = total * 31 + (site == null ? 0 : site.hashCode());
        total = total * 31 + (lemma == null ? 0 : lemma.hashCode());
        return total;
    }

    @Override
    public String toString(){
        return new StringBuilder("\nLemma")
                .append("\n\tID --- ").append(id)
                .append("\n\tSite - ").append(site.getId())
                .append("\n\tLemma - ").append(lemma)
                .append("\n\tFrequency - ").append(frequency)
                .toString();
    }
}
