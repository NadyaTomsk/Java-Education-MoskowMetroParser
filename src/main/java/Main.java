import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static final String URL = "https://www.moscowmap.ru/metro.html#lines";
    public static final String DESTINATION = "src/main/Data";
    public static final String JSON_FILE_NAME = "MoscowMetro.json";
    public static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Connection connection = Jsoup.connect(URL).maxBodySize(0);
            LOGGER.info("Start parse into {}: {}", DESTINATION, URL);
            Document doc = connection.get();
            parseHTML(doc);
        } catch (Exception exception) {
            LOGGER.error("{}: {}", exception.getMessage(), exception.getStackTrace());
        }
    }

    private static void parseHTML(Document doc) {
        Path filePath = Path.of(DESTINATION, JSON_FILE_NAME);
        try {
            MetroParser parser = new MetroParser();
            JSONObject metro = parser.parseMetro(doc);
            Files.createFile(filePath);
            Writer writer = new FileWriter(filePath.toString());
            metro.writeJSONString(writer);
            writer.close();
            LOGGER.info("End parse to file {}", filePath.getFileName());
        } catch (Exception exception) {
            LOGGER.error("{}: {}", exception.getMessage(), exception.getStackTrace());
        }
    }
}
