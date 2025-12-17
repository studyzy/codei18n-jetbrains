package com.github.studyzy.codei18n.utils;

import com.github.studyzy.codei18n.models.CliResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonParserTest {

    @Test
    public void testParseCliResponseWithSourceText() {
        String json = """
            {
              "file": "test_data/sample.rs",
              "comments": [
                {
                  "id": "test123",
                  "file": "test_data/sample.rs",
                  "language": "rust",
                  "symbol": "",
                  "range": {
                    "startLine": 1,
                    "startCol": 1,
                    "endLine": 1,
                    "endCol": 31
                  },
                  "sourceText": "// Calculate fibonacci numbers",
                  "type": "line",
                  "localizedText": "[MOCK] 计算斐波那契数"
                }
              ]
            }
            """;

        CliResponse response = JsonParser.parseCliResponse(json);
        
        assertNotNull(response);
        assertNotNull(response.comments());
        assertEquals(1, response.comments().size());
        
        CliResponse.CommentData comment = response.comments().get(0);
        assertEquals("test123", comment.id());
        assertEquals("// Calculate fibonacci numbers", comment.sourceText());
        assertEquals("[MOCK] 计算斐波那契数", comment.localizedText());
        assertEquals("[MOCK] 计算斐波那契数", comment.getTranslation());
    }
}
