package com.github.kolegran.spdgoogle.search;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchIndexService {
    private static final int FRAGMENT_SIZE = 10;
    private static final int MAX_NUM_FRAGMENTS = 10;

    private final Directory memoryIndex;

    // First of all, this method is long, therefore it's difficult to understand.
    public PageDto searchIndex(String inField, String q, String sortType, int pageNum) {
        String[] fragments = new String[0];

        try {
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexReader indexReader = DirectoryReader.open(memoryIndex);
            IndexSearcher searcher = new IndexSearcher(indexReader);

            Query query = new QueryParser(inField, analyzer).parse(q);
            // Why did you move some numbers to constants, but other used inline?
            // Move all 'magic numbers' to constants with appropriate naming
            TopDocs topDocs = searcher.search(query, pageNum*10, createSort(sortType));

            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), scorer);
            highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, FRAGMENT_SIZE));

            List<Document> documents = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document document = searcher.doc(scoreDoc.doc);

                // same with Strings, it's better to extract constants
                String body = document.get("body");
                // Is there an alternative to the deprecated method? Not critical for the test task though.
                TokenStream stream = TokenSources.getAnyTokenStream(indexReader, scoreDoc.doc, "body", analyzer);
                fragments = highlighter.getBestFragments(stream, body, MAX_NUM_FRAGMENTS);

                documents.add(document);
            }

            List<PageItemDto> pageItems = createPageItem(documents, fragments);
            return PageDto.builder()
                    .numberOfDocs(getAllHitsNumber(query, searcher))
                    .pageItems(pageItems.subList(Math.max(pageItems.size() - 10, 0), pageItems.size()))
                    .build();

        } catch (IOException | ParseException | InvalidTokenOffsetsException e) {
            // throwing generic exception is bad a practice.
            throw new IllegalStateException(e);
        }
    }

    private Sort createSort(String sortType) {
        return sortType.equals("alphabet")
            ? new Sort(new SortField("sortByTitle", SortField.Type.STRING_VAL, false))
            : new Sort();
    }

    private List<PageItemDto> createPageItem(List<Document> documents, String[] fragments) {
        return documents.stream()
                .map(document -> PageItemDto.builder()
                        .url(document.get("url"))
                        .title(document.get("title"))
                        .body(document.get("body"))
                        .fragments(String.join("", fragments))
                        .build())
                .collect(Collectors.toList());
    }

    private int getAllHitsNumber(Query query, IndexSearcher searcher) throws IOException {
        TotalHitCountCollector collector = new TotalHitCountCollector();
        searcher.search(query, collector);
        return collector.getTotalHits();
    }
}
