package com.github.kolegran.spdgoogle.index;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import static com.github.kolegran.spdgoogle.IndexSearchConstants.BODY;
import static com.github.kolegran.spdgoogle.IndexSearchConstants.TITLE;
import static com.github.kolegran.spdgoogle.IndexSearchConstants.URL;
import static com.github.kolegran.spdgoogle.IndexSearchConstants.SORT_BY_FIELD;

@Service
@RequiredArgsConstructor
public class IndexService {
    private final Directory memoryIndex;

    public void indexDocument(Map<String, ParsePageDto> pages) {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(memoryIndex, indexWriterConfig);

            for (Map.Entry<String, ParsePageDto> entry : pages.entrySet()) {
                Document document = new Document();

                document.add(new TextField(URL, entry.getKey(), Field.Store.YES));
                document.add(new TextField(BODY, entry.getValue().getBody(), Field.Store.YES));
                document.add(new TextField(TITLE, entry.getValue().getTitle(), Field.Store.YES));
                document.add(new SortedDocValuesField(SORT_BY_FIELD, new BytesRef(entry.getValue().getTitle())));

                writer.addDocument(document);
            }
            pages.clear();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } finally {
            try {
                writer.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
