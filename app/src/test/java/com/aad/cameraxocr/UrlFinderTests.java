package com.aad.cameraxocr;

import com.aad.cameraxocr.urlfinder.UrlFinder;

import org.junit.Assert;
import org.junit.Test;


public class UrlFinderTests {

    @Test
    public void test_url_finder_and_succeeds(){
        String textWithHtml = "This text containssa\n" +
                "    http://www.url.com\n" +
                "    att wwenv wiwn ekew\n" +
                "    Awgecg\n" +
                "    ww.vwg";

        String expected = "http://www.url.com";

        Assert.assertTrue(UrlFinder.doesURLExists(textWithHtml));

        Assert.assertEquals(expected , UrlFinder.getUrl(textWithHtml));


        textWithHtml = "This text containssa\n" +
                "    att wwenv wiwn ekew\n" +
                "    Awgecg\n" +
                "    http://www.url.com\n" +
                "    ww.vwg";

        Assert.assertTrue(UrlFinder.doesURLExists(textWithHtml));

        Assert.assertEquals(expected , UrlFinder.getUrl(textWithHtml));


        textWithHtml = "wweese\n" +
                "    Mlaevefew\n" +
                "    wtere lietrie Strinlta.stel\n" +
                "    wA\n" +
                "    seriptiauvp4 e etnat.gercelortgercontet\n" +
                "    This text containssa\n" +
                "    http://www.url.com\n" +
                "    h";
        Assert.assertTrue(UrlFinder.doesURLExists(textWithHtml));

        Assert.assertEquals(expected , UrlFinder.getUrl(textWithHtml));
    }
}
