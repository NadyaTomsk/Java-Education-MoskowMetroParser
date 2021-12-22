import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class Station {
    private String name;
    private String number;
    private Line line;

    public final String data_id;

    private TreeMap<String, String> connections = new TreeMap<>();

    private final Random random = new Random();
    public static final Logger LOGGER = LogManager.getLogger(Station.class);

    public Station(String name, String number, Line line, String data_id) {
        this.name = name;
        this.number = number;
        this.line = line;
        this.data_id = data_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Line getLine() {
        return line;
    }

    public void setLine(Line line) {
        this.line = line;
    }

    public TreeMap<String, String> getConnections() {
        return connections;
    }

    public void setConnections(TreeMap<String, String> connections) {
        this.connections = connections;
    }

    public void stationDetails(String URL, String host) {
        try {
            Thread.sleep(random.nextLong(5000, 10000));
            Connection connection = Jsoup.connect(host + URL).maxBodySize(0);
            Document doc = connection.get();
            Element element = doc.select("div.t-text-simple").first();
            if (element == null) {
                return;
            }
            for (Element child : element.children()) {
                String text = child.text();
                if (text.matches("Переход на станцию .*")) {
                    String connectionStationName = child.children().get(0).text();
                    String connectionLineName = child.children().get(1).text();
                    connections.put(connectionStationName, connectionLineName);
                }
            }
        } catch (Exception exception) {
            LOGGER.error("{}: {}", exception.getMessage(), exception.getStackTrace());
        }
    }


}
