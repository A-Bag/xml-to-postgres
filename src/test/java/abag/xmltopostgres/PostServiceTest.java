package abag.xmltopostgres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityManager;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private PostService postService;


    @Test
    public void saveXmlData() throws IOException {

        // given
        ReflectionTestUtils.setField(postService, "batchSize", 50);
        ReflectionTestUtils.setField(postService, "fileLocation", "src/test/resources/Posts.xml");

        // when
        postService.saveXmlData();

        // then
        verify(postRepository, times(71)).save(any(Post.class));
        verify(entityManager, times(1)).clear();
        verify(entityManager, times(1)).flush();
    }

    @Test
    public void testConvertXmlLineToPost() {

        // given
        String line = "<row Id=\"1\" PostTypeId=\"2\" AcceptedAnswerId=\"10\" CreationDate=\"2016-04-26T19:30:14.687\" Score=\"22\" ViewCount=\"678\" Body=\"&lt;p&gt;I've been looking around for some paper for watercolour painting. Watercolour paper can be over double the price of sketch paper.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;What makes watercolour paper better? Should I invest the money, and buy the watercolour paper, or save my money and take a chance on the cheaper option?&lt;/p&gt;&#xA;\" OwnerUserId=\"7\" LastEditorUserId=\"7\" LastEditDate=\"2016-10-27T19:26:38.457\" LastActivityDate=\"2016-10-27T19:26:38.457\" Title=\"What are the advantages of watercolour paper over sketch paper?\" Tags=\"&lt;paper&gt;&lt;watercoloring&gt;&lt;sketching&gt;\" AnswerCount=\"3\" CommentCount=\"2\" />";

        // when
        Post post = postService.convertXmlLineToPost(line);

        // then
        assertEquals(1, post.getId());
        assertEquals(2, post.getPostType());
        assertEquals(22, post.getScore());
        assertEquals(678, post.getViewCount());
        assertEquals(2016, post.getYear());
        assertEquals("What are the advantages of watercolour paper over sketch paper?", post.getTitle());
        assertEquals("&lt;p&gt;I've been looking around for some paper for watercolour painting. Watercolour paper can be over double the price of sketch paper.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;What makes watercolour paper better? Should I invest the money, and buy the watercolour paper, or save my money and take a chance on the cheaper option?&lt;/p&gt;&#xA;", post.getBody());

    }

    @Test
    public void testExtractStringField() {

        // given
        String line = "<row Id=\"1\" PostTypeId=\"1\" AcceptedAnswerId=\"10\" CreationDate=\"2016-04-26T19:30:14.687\" Score=\"22\" ViewCount=\"678\" Body=\"&lt;p&gt;I've been looking around for some paper for watercolour painting. Watercolour paper can be over double the price of sketch paper.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;What makes watercolour paper better? Should I invest the money, and buy the watercolour paper, or save my money and take a chance on the cheaper option?&lt;/p&gt;&#xA;\" OwnerUserId=\"7\" LastEditorUserId=\"7\" LastEditDate=\"2016-10-27T19:26:38.457\" LastActivityDate=\"2016-10-27T19:26:38.457\" Title=\"What are the advantages of watercolour paper over sketch paper?\" Tags=\"&lt;paper&gt;&lt;watercoloring&gt;&lt;sketching&gt;\" AnswerCount=\"3\" CommentCount=\"2\" />";
        String fieldName = "Title";

        // when
        String field = postService.extractStringField(fieldName, line);

        // then
        assertEquals("What are the advantages of watercolour paper over sketch paper?", field);
    }

}
