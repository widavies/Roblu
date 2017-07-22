package com.cpjd.roblu.cloud.api;

import android.os.StrictMode;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

/**
 * The "bridge" between the app and the Roblu Cloud Server. All the methods that
 * the master app needs to use are contained in this class.
 *
 * Here's how returns should be handled:
 * -An exception will be thrown for things like: server is down, file not found, etc.
 * -JSON.get("status") will equal "error" if a specified, user error happened (nothing on our end)
 * -JSON.get("status") will equal "success" if the operation completed, check for data with JSON.get("data")
 *
 * @since 3.6.1
 * @author Will Davies
 */
public class CloudRequest {

    /**
     * The static URL of the Roblu Cloud Server
     */
    private static final String URL = "https://frc-scout-andypethan.c9users.io/";
    private static final JSONParser parser = new JSONParser();

    /**
     * The user's AUTH code, required for authenticating all requests to the server.
     * This is obtained via signIn() and should be stored locally
     */
    private String auth;
    /**
     * This is the team code that the user is a part of. Both this and an auth token are required for
     * modifying a team's data
     */
    private String teamCode;

    public CloudRequest() {}

    public CloudRequest(String auth, String teamCode) {
        this.auth = auth;
        this.teamCode = teamCode;
    }

    /**
     * Attempts to sign the user into their Roblu Cloud account. If their account already exists, it is returned,
     * if not, a new account is created and returned.
     * @param name The user's Google display name
     * @param email The user's Google email address
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object signIn(String name, String email) throws Exception {
        return doRequest(false, "users/signIn", "?name="+encodeString(name)+"&email="+encodeString(email));
    }

    /**
     * Regenerates the team's token, note, this will effectively kick all users on the team because their old
     * team code will no longer work for sending data.
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object regenerateToken() throws Exception {
        return doRequest(false, "teams/regenerateToken","?auth="+encodeString(auth)+"&code="+encodeString(teamCode));
    }

    /**
     * Makes sure that only ONE user can be signed into the master account at once.
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object joinTeam() throws Exception {
        return doRequest(false, "teams/joinTeam", "?auth="+encodeString(auth)+"&code="+encodeString(teamCode));
    }
    /**
     * Makes sure that only ONE user can be signed into the master account at once.
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object leaveTeam() throws Exception {
        return doRequest(false, "teams/leaveTeam", "?auth="+encodeString(auth)+"&code="+encodeString(teamCode));
    }

    /**
     * Attempts to push the form to the server
     * @param content JSON representation of form
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object pushForm(String content) throws Exception {
        return doRequest(true, "teams/pushForm", "&content="+encodeString(content)+
                "&code="+encodeString(teamCode)+"&auth="+encodeString(auth));
    }

    /**
     * Overwrites all old data on the server and pushes all the new checkouts
     * @param content the serialized array of RCheckout models
     * @param activeEventTitle the title of the currently being pushed event
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object initPushCheckouts(String activeEventTitle, String content) throws Exception {
        return doRequest(true, "checkouts/initPushCheckouts", "?content="+encodeString(content)+
                "&code="+encodeString(teamCode)+"&auth="+encodeString(auth)+"&active="+encodeString(activeEventTitle));
    }

    /**
     * Pushes the checkout to the Checkouts database and overwrites the old one
     * @param id the checkout to overwrite
     * @param status the status of the checkout ('completed')
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object pushCheckout(int id, String status, String content) throws Exception {
        return doRequest(true, "checkouts/pushCheckout", "?content="+encodeString(content)+"&status="+encodeString(status)+"&id="+encodeString(String.valueOf(id))+
                "&code="+encodeString(teamCode)+"&auth="+encodeString(auth));
    }

    /**
     * Clears the active event from the server (deletes Checkouts db, InCheckouts db, and active event tag)
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object clearActiveEvent() throws Exception {
        return doRequest(false, "checkouts/clearActiveEvent", "?code="+encodeString(teamCode));
    }

    /**
     * Pulls checkouts from the InCheckouts database
     * @return object representing the servers response (either success or error)
     * @throws Exception A more broad error happened, server could not be contacted, wrong parameters or URL, response could not be read, etc.
     */
    public Object pullCheckouts() throws Exception {
        return doRequest(false, "checkouts/pullReceivedCheckouts","?code="+encodeString(teamCode));
    }


    /**
     * Performs a request to the server.
     * If for whatever reason a response is not received from the server, an exception is thrown,
     * if a response from the server is received, then this should be handled in a higher up method.
     *
     * @param post whether to use post or get method
     * @param targetURL the location url eg 'users/signIn'
     * @param parameters the parameters, will be adjusted for GET or POST, actual parameter content should be encoded with encoded method
     * @throws IOException response could not be processed
     * @throws ParseException response could not be process
     */
    public static Object doRequest(boolean post, String targetURL, String parameters) throws IOException, ParseException {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build(); StrictMode.setThreadPolicy(policy);

        URL url;
        if(post) url = new URL(URL+ targetURL);
        else url = new URL(URL+targetURL+parameters);

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        if(post) {
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
        }
        //connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "utf-8");
        connection.setUseCaches(false);
        if(post) {
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write("&"+parameters.substring(1));
            wr.flush();
        }
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rd.close();
        if(connection != null) connection.disconnect();
        Object toReturn = parser.parse(response.toString());
        if(toReturn == null) throw new IOException();
        return toReturn;
    }

    public static String encodeString(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            return "null";
        }
    }

}
