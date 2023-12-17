package searchengine.exceptions;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.index.IndexResponse;

@ControllerAdvice
public class Advice {
    @ExceptionHandler({IndexingAlreadyRunningException.class, IncorrectSiteAddressException.class, IndexingIsNotRunningException.class})
    public IndexResponse exceptionHandler(IndexingAlreadyRunningException e){
        return new IndexResponse(false,e.getMessage());
    }
//    @ExceptionHandler(IndexingAlreadyRunningException.class)
//    public ErrorResponse indexingAlreadyRunningExceptionHandler(IndexingAlreadyRunningException e){
//        ErrorResponse errorResponse = new ErrorResponse(false,e.getMessage());
//        return errorResponse;
//    }
//    @ExceptionHandler(IncorrectSiteAddressException.class)
//    public ErrorResponse incorrectSiteAddressExceptionHandler(IncorrectSiteAddressException e){
//        ErrorResponse errorResponse = new ErrorResponse(false,e.getMessage());
//        return errorResponse;
//    }
//    @ExceptionHandler(IndexingIsNotRunningException.class)
//    public ErrorResponse indexingIsNotRunningExceptionHandler(IndexingIsNotRunningException e){
//        ErrorResponse errorResponse = new ErrorResponse(false,e.getMessage());
//        return errorResponse;
//    }

}
