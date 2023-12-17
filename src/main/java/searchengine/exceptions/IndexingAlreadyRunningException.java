package searchengine.exceptions;

public class IndexingAlreadyRunningException extends RuntimeException{
    public IndexingAlreadyRunningException(){
        super("Indexing is already running");
    }
}
