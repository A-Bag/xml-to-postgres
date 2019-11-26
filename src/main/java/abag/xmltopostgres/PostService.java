package abag.xmltopostgres;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Service
public class PostService {

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Value("${xml.file.location}")
    private String fileLocation;

    private PostRepository postRepository;
    private EntityManager entityManager;

    public PostService(PostRepository postRepository, EntityManager entityManager) {
        this.postRepository = postRepository;
        this.entityManager = entityManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void saveXmlData() throws IOException {

        int rowNumber = 0;

        try (LineIterator lineIterator = FileUtils.lineIterator(new File(fileLocation))) {

            Instant saveStart = Instant.now();

            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                if (line.contains("<row")) {
                    Post post = convertXmlLineToPost(line);
                    rowNumber++;
                    postRepository.save(post);

                    if (rowNumber % batchSize == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                    if (rowNumber % 100_000 == 0) {
                        System.out.println(rowNumber + " / 10 000 000 rows inserted");
                    }

                    if (rowNumber == 10_000_000) {
                        break;
                    }
                }
            }
            Instant saveEnd = Instant.now();

            long saveTime = Duration.between(saveStart, saveEnd).toMillis();

            System.out.println("Save time: " + saveTime + "ms");

        }
    }

    public Post convertXmlLineToPost(String line) {
        Post post = new Post();

        post.setId(Integer.parseInt(extractStringField("Id", line)));

        if (line.contains("PostTypeId=\"")) {
            post.setPostType(Integer.parseInt(extractStringField("PostTypeId=\"", line)));
        }

        if (line.contains("Score=\"")) {
            post.setScore(Integer.parseInt(extractStringField("Score", line)));
        }

        if (line.contains("ViewCount=\"")) {
            post.setViewCount(Integer.parseInt(extractStringField("ViewCount=\"", line)));
        }

        if (line.contains("CreationDate=\"")) {
            String date = extractStringField("CreationDate=\"", line);
            int year = Integer.parseInt(date.substring(0, 4));
            post.setYear(year);
        }

        if (line.contains("Body=\"")) {
            String rawBody = extractStringField("Body=\"", line);
            String bodyText = removeHTMLFromString(rawBody);
            post.setBody(Jsoup.parse(bodyText).text());
        }

        if (line.contains("Title=\"")) {
            String rawTitle = extractStringField("Title=\"", line);
            String titleText = removeHTMLFromString(rawTitle);
            post.setTitle(titleText);
        }

        return post;
    }

    public String extractStringField(String fieldName, String line) {
        int beginFieldIndex = line.indexOf(fieldName);
        int beginIndex = line.indexOf("\"", beginFieldIndex) + 1;
        int endIndex = line.indexOf("\"", beginIndex);
        return line.substring(beginIndex, endIndex);
    }

    private String removeHTMLFromString(String string) {
        String htmlEscapedString = StringEscapeUtils.unescapeHtml4(string);
        return Jsoup.parse(htmlEscapedString).text();
    }
}
