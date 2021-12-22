import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.*;

public class MetroParser {

    //private String host;

    private final ArrayList<Station> stations = new ArrayList<>();
    private final ArrayList<Line> lines = new ArrayList<>();
    private final List<Pair<Station, Station>> connections = new ArrayList<>();

    public static final Logger LOGGER = LogManager.getLogger(MetroParser.class);

    public MetroParser() {
    }

    public ArrayList<Station> getStations() {
        return stations;
    }

    public ArrayList<Line> getLines() {
        return lines;
    }

    public JSONObject parseMetro(Document doc) {
        Elements elements = doc.select("span.js-metro-line");
        //host = doc.connection().request().url().getProtocol() +"://" + doc.connection().request().url().getHost();
        Elements stationsList = doc.select("div.js-metro-stations");
        JSONArray linesJSON = parseLine(elements);
        JSONArray stationsJSON = parseStations(stationsList);
        //JSONArray connectionsJSON = parseConnections();
        JSONArray connectionsJSON = parseConnectionBySVG(Objects.requireNonNull(doc.select("g.p2p2").first()));
        JSONObject metro = new JSONObject();
        metro.put("stations", stationsJSON);
        metro.put("lines", linesJSON);
        metro.put("connections", connectionsJSON);
        return metro;
    }

    private JSONArray parseLine(Elements elements) {
        JSONArray linesJSON = new JSONArray();
        for (Element element : elements) {
            String lineNumber = element.attr("data-line");
            String lineName = element.text();
            JSONObject line = new JSONObject();
            line.put("number", lineNumber);
            line.put("name", lineName);
            linesJSON.add(line);
            lines.add(new Line(lineNumber, lineName));
            LOGGER.info("Линия №{} - {}", lineNumber, lineName);
        }
        LOGGER.info("Lines are parsed");
        return linesJSON;
    }

    private JSONArray parseStations(Elements elements) {
        JSONArray stationsList = new JSONArray();
        for (Element element : elements) {
            String lineNumber = element.attr("data-line");
            Elements stationsElements = element.getElementsByTag("a");
            for (Element station : stationsElements) {
                String stationNumber = Objects.requireNonNull(station.getElementsByClass("num").first()).text();
                String stationName = Objects.requireNonNull(station.getElementsByClass("name").first()).text();
                String stationId = station.attr("data-metrost").split(",")[0];
                JSONObject s = new JSONObject();
                s.put("line", lineNumber);
                s.put("number", stationNumber);
                s.put("name", stationName);
                Optional<Line> line = lines.stream().filter(l -> lineNumber.equals(l.getNumber())).findFirst();
                Station st = new Station(stationName, stationNumber, line.get(), stationId);
                //st.stationDetails(stationWeb, host);
                stations.add(st);
                stationsList.add(s);
            }
        }
        LOGGER.info("Stations are parsed");
        return stationsList;
    }

    private JSONArray parseConnectionBySVG(Element element){
        JSONArray connectionsJSON = new JSONArray();
        Elements elements = element.children();
        for(Element conn : elements){
            String txt = conn.className();
            String[] list = txt.split("\\s");
            if(list.length>2){
                continue;
            }
            Optional<Station> optionalStation1 = stations.stream()
                                        .filter(s-> s.data_id.equals(list[0].replaceAll("p","")))
                                        .findFirst();
            Optional<Station> optionalStation2 = stations.stream()
                                        .filter(s-> s.data_id.equals(list[1].replaceAll("p","")))
                                        .findFirst();
            if(optionalStation1.isPresent() && optionalStation2.isPresent()){
                Station station1 = optionalStation1.get();
                Station station2 = optionalStation2.get();
                connections.add(new ImmutablePair<>(station1, station2));
                LOGGER.info("Add connection: {} line {} with {} line {}",
                        station1.getName(), station1.getLine().getName(),
                        station2.getName(), station2.getLine().getName());
                JSONObject s1 = new JSONObject();
                s1.put("name", station1.getName());
                s1.put("number", station1.getNumber());
                s1.put("line", station1.getLine().getName());
                JSONObject s2 = new JSONObject();
                s2.put("name", station2.getName());
                s2.put("number", station2.getNumber());
                s2.put("line", station2.getLine().getName());
                JSONObject connection = new JSONObject();
                connection.put("station1", s1);
                connection.put("station2", s2);
                connectionsJSON.add(connection);
            }
        }
        return  connectionsJSON;
    }


    private JSONArray parseConnections() {
        JSONArray connectionsJSON = new JSONArray();
        for (Station station : stations) {
            if(station.getConnections().size() == 0){
                continue;
            }
            Optional<Station> connectedStation = stations.stream()
                    .filter(s-> station.getConnections().get(s.getName()) != null)
                    .findFirst();
            if(connectedStation.isPresent()) {
                Station cStation = connectedStation.get();
                connections.add(new ImmutablePair<>(station, cStation));
                JSONObject s1 = new JSONObject();
                s1.put("name", station.getName());
                s1.put("number", station.getNumber());
                s1.put("line", station.getLine().getName());
                JSONObject s2 = new JSONObject();
                s2.put("name", cStation.getName());
                s2.put("number", cStation.getNumber());
                s2.put("line", cStation.getLine().getName());
                JSONObject connection = new JSONObject();
                connection.put("station1", s1);
                connection.put("station2", s2);
                connectionsJSON.add(connection);
            }
        }
        LOGGER.info("Connections are parsed");
        return connectionsJSON;
    }
}
