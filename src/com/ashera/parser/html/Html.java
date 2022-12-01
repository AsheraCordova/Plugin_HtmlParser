package com.ashera.parser.html;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ashera.attributedtext.AttributedString;
import com.ashera.core.IFragment;

import repackaged.org.ccil.cowan.tagsoup.HTMLSchema;
import repackaged.org.ccil.cowan.tagsoup.TagSoupParser;


/**
 * This class processes HTML strings into displayable styled text.
 * Not all HTML tags are supported.
 */
public class Html {
	public static final String UNORDERED_LIST = "ul";
    public static final String ORDERED_LIST = "ol";
    public static final String LIST_ITEM = "li";

    private Html() { }



    /**
     * Lazy initialization holder for HTML parser. This class will
     * a) be preloaded by the zygote, or b) not loaded until absolutely
     * necessary.
     */
    private static class HtmlParser {
        private static final HTMLSchema schema = new HTMLSchema();
    }

    /**
     * Returns displayable styled text from the provided HTML string.
     * Any &lt;img&gt; tags in the HTML will use the specified ImageGetter
     * to request a representation of the image (use null if you don't
     * want this) and the specified TagHandler to handle unknown tags
     * (specify null if you don't want this).
     *
     * <p>This uses TagSoup to handle real HTML, including all of the brokenness found in the wild.
     * @param htmlConfig 
     */
    public static AttributedString fromHtml(String source, Map<String, Object> htmlConfig, IFragment fragment) {
    	if (source == null) {
    		source = "";
    	}
    	TagSoupParser parser = new TagSoupParser();
        try {
            parser.setProperty(TagSoupParser.schemaProperty, HtmlParser.schema);
        } catch (org.xml.sax.SAXNotRecognizedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        } catch (org.xml.sax.SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        }

        HtmlToSpannedConverter converter = new HtmlToSpannedConverter(source, parser, htmlConfig, fragment);
        return converter.convert();
    }
    
    public static void parseHtml(String source, ContentHandler contentHandler) {
    	if (source == null) {
    		source = "";
    	}
    	TagSoupParser parser = new TagSoupParser();
        try {
            parser.setProperty(TagSoupParser.schemaProperty, HtmlParser.schema);
            parser.setContentHandler(contentHandler);
            parser.parse(new InputSource(new StringReader(source)));
        } catch (org.xml.sax.SAXNotRecognizedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        } catch (org.xml.sax.SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        } catch (IOException e) {
            // We are reading from a string. There should not be IO problems.
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // TagSoup doesn't throw parse exceptions.
            throw new RuntimeException(e);
        }
    }

}