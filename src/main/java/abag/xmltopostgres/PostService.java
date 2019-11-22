package abag.xmltopostgres;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;

@Service
public class PostService {

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

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

        try (LineIterator lineIterator = FileUtils.lineIterator(new File("/media/sf_Downloads/crafts.stackexchange.com/Posts.xml"))) {

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
                }
            }

        }
    }

    public Post convertXmlLineToPost(String line) {
        Post post = new Post();

        post.setId(Integer.parseInt(extractStringField("Id", line)));

        if (line.contains("PostTypeId")) {
            post.setPostType(Integer.parseInt(extractStringField("PostTypeId", line)));
        }

        if (line.contains("Score")) {
            post.setScore(Integer.parseInt(extractStringField("Score", line)));
        }

        if (line.contains("ViewCount")) {
            post.setViewCount(Integer.parseInt(extractStringField("ViewCount", line)));
        }

        if (line.contains("CreationDate")) {
            String date = extractStringField("CreationDate", line);
            int year = Integer.parseInt(date.substring(0, 4));
            post.setYear(year);
            post.setBody(extractStringField("Body", line));
        }

        if (line.contains("Title")) {
            post.setTitle(extractStringField("Title", line));
        }

        return post;
    }

    public String extractStringField(String fieldName, String line) {
        int beginFieldIndex = line.indexOf(fieldName);
        int beginIndex = line.indexOf("\"", beginFieldIndex) + 1;
        int endIndex = line.indexOf("\"", beginIndex);
        return line.substring(beginIndex, endIndex);
    }
}
