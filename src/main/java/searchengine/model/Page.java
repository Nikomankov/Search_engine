package searchengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import org.hibernate.annotations.Cascade;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "`path`", columnDefinition = "varchar(250)", nullable = false)
    private String path;

    @Column(name = "`code`", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "mediumtext", nullable = false)
    private String content;

    @Tolerate
    public Page(){}

    @Override
    public boolean equals(Object obj){
        Page page = (Page)obj;
        return page.getPath().equals(this.path);
    }

    @Override
    public int hashCode(){
        int total = 31;
        total = total * 31 + id;
        total = total * 31 + (path == null ? 0 : path.hashCode());
        total = total * 31 + (site == null ? 0 : site.hashCode());
        return total;
    }

    @Override
    public String toString(){
        return new StringBuilder("\nPage")
                .append("\n\tID --- ").append(id)
                .append("\n\tPath - ").append(path)
                .append("\n\tCode - ").append(code)
                .append("\n\tSite - ").append(site.getName())
                .toString();
    }
}
