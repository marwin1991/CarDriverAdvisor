package pl.edu.agh.car_driver_advisor.carvelocity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

class RouteDataProvider {
    private final static String API_CALL_PATTERN =
            "https://z.overpass-api.de/api/xapi?way[bbox=%f,%f,%f,%f][maxspeed=*]";
    private final static double HALF_SQUARE_SIDE_LENGTH_DEGREES = 0.00025;
    private final static float CONVERT_MPH_TO_KMH = 1.609344f;

    private final DocumentBuilderFactory documentBuilderFactory;

    RouteDataProvider() {
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
    }

    /**
     * Returns speed limit of road which is contained in square between given cords
     * (see overpass api - bbox).
     * When there are multiple roads with 'maxspeed' parameter satisfying request,
     * the lowest speed value will be returned in kilometers per hour.
     *
     * @param latitude
     * @param longitude
     * @return Optional, possibly with road speed limit
     */
    Optional<Integer> getAllowedSpeedForRouteWithGivenCords(double latitude, double longitude) {
        Document document;

        try {
            URL url = new URL(constructQueryToApi(latitude, longitude));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.connect();

            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
            document = docBuilder.parse(urlConnection.getInputStream());

            urlConnection.disconnect();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            return Optional.empty();
        }

        NodeList nodesWithWayTag = document.getElementsByTagName("way");
        return IntStream.range(0, nodesWithWayTag.getLength())
                .mapToObj(nodesWithWayTag::item)
                .map(node -> ((Element) node).getElementsByTagName("tag"))
                .map(this::getWayMaxSpeed)
                .filter(Optional::isPresent)    // or java 9 flatMap(Optional::stream)
                .map(Optional::get)
                .min(Integer::compareTo);
    }

    private String constructQueryToApi(double latitude, double longitude) {
        double north = latitude + HALF_SQUARE_SIDE_LENGTH_DEGREES;
        double south = latitude - HALF_SQUARE_SIDE_LENGTH_DEGREES;
        double east = longitude + HALF_SQUARE_SIDE_LENGTH_DEGREES;
        double west = longitude - HALF_SQUARE_SIDE_LENGTH_DEGREES;

        return String.format(Locale.US, API_CALL_PATTERN, west, south, east, north);
    }

    private Optional<Integer> getWayMaxSpeed(NodeList wayInfoTags) {
        return IntStream.range(0, wayInfoTags.getLength())
                .mapToObj(wayInfoTags::item)
                .map(Node::getAttributes)
                .filter(o -> o.getNamedItem("k").getNodeValue().equals("maxspeed"))
                .map(o -> o.getNamedItem("v").getNodeValue())
                .map(this::convertSpeedToNumeric)
                .findFirst();
    }

    private Integer convertSpeedToNumeric(String speed) {
        if(speed.contains("mph")) {
            return Math.round(Integer.parseInt(
                    speed.replace("mph", "").trim()) * CONVERT_MPH_TO_KMH
            );
        }
        return Integer.parseInt(speed);
    }

}
