package com.shortify;

import com.shortify.model.UrlEntity;
import com.shortify.service.AiService;
import com.shortify.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AiProcessorTest {

    @Autowired
    private UrlService urlService;

    @Autowired
    private AiService aiService;

    @Test
    void testAiProcessing() throws InterruptedException {
        String testShortCode = "hHdBQk";

        Optional<UrlEntity> optionalUrl = urlService.getUrlEntityByShortCode(testShortCode);
        assertTrue(optionalUrl.isPresent(), "URL must exist in DB");

        UrlEntity url = optionalUrl.get();
        System.out.println("Before AI: aiChecked = " + url.getAiChecked() + ", riskLevel = " + url.getRiskLevel());

        // Trigger AI processing (synchronous or async)
        aiService.analyzeUrl(url);

        // Wait if async
        Thread.sleep(1000);

        // Fetch updated entity
        UrlEntity updatedUrl = urlService.getUrlEntityByShortCode(testShortCode)
                .orElseThrow(() -> new RuntimeException("URL not found after AI"));

        System.out.println("After AI: aiChecked = " + updatedUrl.getAiChecked() + ", riskLevel = " + updatedUrl.getRiskLevel());

        // Assertions
        assertTrue(updatedUrl.getAiChecked(), "AI should mark URL as checked");
        assertNotEquals(null, updatedUrl.getRiskLevel(), "Risk level should be set by AI");
    }
}
