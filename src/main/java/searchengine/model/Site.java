package searchengine.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Builder
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')")
    private IndexingStatus status;
    @Column(columnDefinition = "DATETIME", nullable = false)
    private Date status_time;
    @Column(columnDefinition = "TEXT")
    private String last_error;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @Override
    public String toString(){
        return new StringBuilder("\nSite ")
                .append(name != null        ? "\n  Name -------- " + name : "")
                .append(id != null          ? "\n  ID ---------- " + id : "")
                .append(url != null         ? "\n  URL --------- " + url : "")
                .append(status != null      ? "\n  Status ------ " + status : "")
                .append(status_time != null ? "\n  Time -------- " + status_time : "")
                .append(last_error != null  ? "\n  Last error: - " + last_error : "")
                .toString();
    }
}
