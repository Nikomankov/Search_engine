package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.services.IndexService;

@RestController
@RequestMapping("/api")
public class IndexController {
    @Autowired
    private IndexService indexService;

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing(){
        return ResponseEntity.ok(indexService.start());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing(){
        return ResponseEntity.ok(indexService.stop());
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(String url){
        return ResponseEntity.ok(indexService.indexPage(url));
    }

}
