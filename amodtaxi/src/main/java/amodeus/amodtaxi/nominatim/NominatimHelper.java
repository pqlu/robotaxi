/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodtaxi.nominatim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

public final class NominatimHelper {
    private static final Logger LOGGER = Logger.getLogger(NominatimHelper.class);

    private NominatimHelper() { }

    public static String queryInterface(String https_url) {
        String returnString = "";
        try {
            URL url = new URL(https_url);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            returnString = readInterfaceResponse(con);
            con.disconnect();
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed URL: " + https_url, e);
        } catch (Exception e) {
            LOGGER.error("Failed to query Nominatim: " + https_url, e);
        }
        return returnString;
    }

    private static String readInterfaceResponse(HttpsURLConnection con) {
        StringBuilder result = new StringBuilder();
        if (Objects.nonNull(con)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String input;
                while ((input = br.readLine()) != null) {
                    result.append(input);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read Nominatim response", e);
            }
        }
        return result.toString();
    }
}