
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 *
 * @author marcos
 * @time 7:46:46 PM
 */
public class Test {

    public static void main(String[] args) throws IOException, XMLStreamException {
        Document doc = Jsoup.parse(new FileInputStream("allagents.xml"), "UTF-8","", Parser.xmlParser());
        Elements agents = doc.select("user-agent > String");
        for (int i = 0; i < 10; i++) {
            System.out.println(agents.get((int) (Math.random() * agents.size())).text());
        }        
        
    }

}
