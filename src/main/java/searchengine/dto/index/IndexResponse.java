package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class IndexResponse {
    private boolean result;
    private String error;

    public IndexResponse(boolean result){
        this.result = result;
    }
}
