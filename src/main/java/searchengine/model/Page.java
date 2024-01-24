package searchengine.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Builder
@DynamicUpdate
@Entity
@Table(name="page", indexes = {@Index(name = "index_page", columnList = "`path`")})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "`path`", columnDefinition = "varchar(250)", nullable = false)
    private String path;

    @Column(name = "`code`", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "mediumtext", nullable = false)
    private String content;

    @Override
    public String toString(){
        return new StringBuilder("\nPage")
                .append("\nID --- ").append(id)
                .append("\nPath - ").append(path)
                .append("\nCode - ").append(code)
                .append("\nSite - ").append(site)
                .toString();
    }
}
