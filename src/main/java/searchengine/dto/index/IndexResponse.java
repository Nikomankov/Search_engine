package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class IndexResponse {
    private boolean result;
    private String error;

    public IndexResponse(boolean result){
        this.result = result;
    }
    public IndexResponse(boolean result, String error){
        this.result = result;
        this.error = error;
    }
}
