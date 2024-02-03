package searchengine.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.hibernate.annotations.DynamicUpdate;

import java.text.SimpleDateFormat;
import java.sql.Date;

@Data
@Builder
@DynamicUpdate
@Entity
public class Site implements Cloneable{

    private static SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "`status`", columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')")
    private IndexingStatus status;


    @Setter
    @Getter
    @Column(name = "status_time", columnDefinition = "datetime", nullable = false)
    private Date statusTime;

    @Getter
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Getter
    @Column(name = "`url`", columnDefinition = "varchar(255)", nullable = false)
    private String url;

    @Getter
    @Column(name = "`name`", columnDefinition = "varchar(255)", nullable = false)
    private String name;

    @Tolerate
    public Site(){}

    @Override
    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }

    @Override
    public boolean equals(Object obj){
        Site site = (Site)obj;
        return site.getUrl().equals(this.url);
    }

    @Override
    public int hashCode(){
        int total = 31;
        total = total * 31 + id;
        total = total * 31 + (url == null ? 0 : url.hashCode());
        return total;
    }

    @Override
    public String toString(){
        return new StringBuilder("\nSite ")
                .append(name != null        ? "\n\tName -------- " + name : "")
                .append("\n\tID ---------- " + id)
                .append(url != null         ? "\n\tURL --------- " + url : "")
                .append(status != null      ? "\n\tStatus ------ " + status : "")
                .append(statusTime != null ? "\n\tTime -------- " + format.format(statusTime) : "")
                .append(lastError != null  ? "\n\tLast error: - " + lastError : "")
                .toString();
    }
}
