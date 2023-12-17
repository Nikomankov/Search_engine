package searchengine.exceptions;

public class IncorrectSiteAddressException extends RuntimeException{
    public IncorrectSiteAddressException(){
        super("This page is located outside the sites specified in the configuration file");
    }
}
