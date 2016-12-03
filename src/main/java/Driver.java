import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.io.FileReader;

public class Driver {
    /**
     * Credit to: http://stackoverflow.com/questions/18455394/java-function-that-accepts-address-and-returns-longitude-and-latitude-coordinate
     */
    public static String[] getLatLongPositions(String address) throws Exception
    {
        int responseCode = 0;
        String api = "http://maps.googleapis.com/maps/api/geocode/xml?address=" + URLEncoder.encode(address, "UTF-8") + "&sensor=true";
        System.out.println("URL : "+api);
        URL url = new URL(api);
        HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
        httpConnection.connect();
        responseCode = httpConnection.getResponseCode();
        if(responseCode == 200)
        {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();;
            Document document = builder.parse(httpConnection.getInputStream());
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/GeocodeResponse/status");
            String status = (String)expr.evaluate(document, XPathConstants.STRING);
            if(status.equals("OK"))
            {
                expr = xpath.compile("//geometry/location/lat");
                String latitude = (String)expr.evaluate(document, XPathConstants.STRING);
                expr = xpath.compile("//geometry/location/lng");
                String longitude = (String)expr.evaluate(document, XPathConstants.STRING);
                return new String[] {latitude, longitude};
            }
            else
            {
                throw new Exception("Error from the API - response status: "+status);
            }
        }
        return null;
    }
    /*
     * Takes in a data file in correct format;
      *
      *     Center name
      *     Center address
      *     Center country (..?)
      *     sCenter Details (split by - )
      *
      * /and writes to json array to output file
     */
    public static void parse(String input, String output) throws Exception {

        /* Prepare ArrayList of TraumaCenter objects */
        ArrayList<TraumaCenter> centers = new ArrayList<TraumaCenter>();

        /* Attempt to open the given file */
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader("src/main/centers.txt"));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return;
        }

        String line = "";


        /* Read all lines from the file that aren't empty */
            while ((line = br.readLine()) != null) {

                // Iterate over any empty lines
                try
                {
                    while ((line = br.readLine()).trim().equals("")) ;
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    break;
                }

                // Remove preceding and proceeding whitespace
                line = line.trim();

                // Get Name
                System.out.println("Name: " + line);
                String name = line;

                // Iterate over any empty lines
                while ((line = br.readLine()).trim().equals("")) ;

                // Remove preceding and proceeding whitespace
                line = line.trim();

                String[] coordinates = null;

                // Try and get co-ordinates
                while(true)
                {
                    try
                    {
                        coordinates = getLatLongPositions(line);
                        System.out.println(coordinates[0] + " , " + coordinates[1]);
                        break;
                    }
                    catch(Exception e)
                    {
                        // May run into OVER_QUERY_LIMIT, sleep thread for 3s and try again
                        e.printStackTrace();
                        Thread.sleep(3000);
                    }
                }

                // Iterate over any empty lines
                while ((line = br.readLine()).trim().equals("")) ;

                // Iterate over United States
                line = br.readLine();
                System.out.println("details: " + line);

                // Get Trauma Center Level Details. Separated by -
                String[] details = line.split("-");
                details[1] = details[1].trim();

                System.out.println(details[1]);

                // Create new object < name, level, latitude, longitude >
                // and add to the list.
                centers.add(new TraumaCenter(name, details[1], Double.parseDouble(coordinates[0]),
                        Double.parseDouble(coordinates[1])));

                // Iterate over any empty lines
            }

        // Use Gson to turn the ArrayList into a JSON String
        String json = new Gson().toJson(centers);

        // Make the JSON String pretty, of course
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);

        // To JSON
        json = gson.toJson(je);
        System.out.println(json);

        // Write the resulting JSON string to the specified output file
        BufferedWriter bw = new BufferedWriter(new FileWriter(output));
        bw.write(json);
    }

    public static void main(String[] args) throws Exception {
        // Call Parse method (input, output)
        parse("/Users/andrew/Desktop/centers.txt", "json.txt");
    }
}
class TraumaCenter
{
    public String name;
    public String level;
    public double latitude;
    public double longitude;

    public TraumaCenter(String name, String level, double latitude, double longitude)
    {
        this.level = level;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}