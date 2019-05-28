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

@Service
@RequiredArgsConstructor
public class IndexService {
    private final Directory memoryIndex;

    public void indexDocument(Map<String, ParsePageDto> pages) {
        try {
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(memoryIndex, indexWriterConfig);

            // possible https://docs.oracle.com/javase/7/docs/api/java/util/ConcurrentModificationException.html
            // In case multiple users would use com.github.kolegran.spdgoogle.index.IndexController.createIndex simultaneously
            for (Map.Entry<String, ParsePageDto> entry : pages.entrySet()) {
                Document document = new Document();

                document.add(new TextField("url", entry.getKey(), Field.Store.YES));
                document.add(new TextField("body", entry.getValue().getBody(), Field.Store.YES));
                document.add(new TextField("title", entry.getValue().getTitle(), Field.Store.YES));
                document.add(new SortedDocValuesField("sortByTitle", new BytesRef(entry.getValue().getTitle())));

                writer.addDocument(document);
            }
            pages.clear();
            // if any exception is thrown between lines 28 and 43 then writer would never close,
            // consider using try-with-resources as IndexWriter implements java.io.Closeable
            writer.close();
        } catch (IOException e) {
            // Wrapping checked exception into generic unchecked exception is considered a bad practice
            // Declare and throw a dedicated exception with meaningful message instead of generic IllegalStateException
            // see http://cwe.mitre.org/data/definitions/397.html
            throw new IllegalStateException(e);
        }
    }
}
