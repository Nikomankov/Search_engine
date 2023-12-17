package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import javax.persistence.Index;

@Data
@Entity
@Table(name = "page", indexes = {@Index(columnList = "path", unique = true)})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;
    @Column(nullable = false)
    private Integer code;
    @Column(columnDefinition = "TEXT", nullable = false)
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
