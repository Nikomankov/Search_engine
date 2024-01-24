package searchengine.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Date;

@Data
@Builder
@DynamicUpdate
@Entity
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "`status`", columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')")
    private IndexingStatus status;

    @Column(name = "status_time", columnDefinition = "datetime", nullable = false)
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "`url`", columnDefinition = "varchar(255)", nullable = false)
    private String url;

    @Column(name = "`name`", columnDefinition = "varchar(255)", nullable = false)
    private String name;

    @Override
    public String toString(){
        return new StringBuilder("\nSite ")
                .append(name != null        ? "\n  Name -------- " + name : "")
                .append("\n  ID ---------- " + id)
                .append(url != null         ? "\n  URL --------- " + url : "")
                .append(status != null      ? "\n  Status ------ " + status : "")
                .append(statusTime != null ? "\n  Time -------- " + statusTime : "")
                .append(lastError != null  ? "\n  Last error: - " + lastError : "")
                .toString();
    }
}
