package com.github.kolegran.spdgoogle.index;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class HttpService {
    public Document downloadDocument(String url) throws IOException {
        // good thing that you did not write another bicycle
        return Jsoup.connect(url).get();
    }
}
