package invin.com.similarmovies.util;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility that contains some helper HTTP methods
 */
public final class HTTPUtil {

    /**
     * Makes the HTTP call to the API & returns the JSON response as a {@link java.lang.String} or
     *  Returns a {@link java.lang.String} containing the JSON consisting of similar movies returned
     *  from the API based n the movie ID sent in.
     *
     * @param searchByMovieIdOrName
     *          1 - Search with the provided movie name
     *          2 - Search with the provided movie ID.
     * @param movieIdOrName {@link java.lang.String} consisting of the movie name
     * @param apiKey {@link java.lang.String} consisting of the API key
     * @return {@link java.lang.String} consisting of the JSON response from the Rotten Tomatoes API
     */
    public static String returnSearchResultOrSimilarMoviesJSON(int searchByMovieIdOrName, String movieIdOrName, String apiKey) {
        URL url = null;
        StringBuffer responseString = null;
        HttpURLConnection httpURLConnection = null;

        try {
            if(searchByMovieIdOrName == Constants.SEARCH_BY_MOVIE_NAME) {
                url = new URL(
                        Constants.API_URL_PREPENDER +
                                Constants.API_MOVIE_SEARCH_QUERY_APPENDER +
                                movieIdOrName +
                                Constants.API_MOVIE_SEARCH_CONNECTOR +
                                apiKey);
            } else if (searchByMovieIdOrName == Constants.SEARCH_BY_MOVIE_ID) {
                url = new URL(
                        Constants.API_URL_PREPENDER +
                                Constants.FORWARD_SLASH +
                                movieIdOrName +
                                Constants.FORWARD_SLASH +
                                Constants.API_URL_SIMILAR_CONNECTOR +
                                apiKey);
            }

            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");

            Log.d(HTTPUtil.class.getCanonicalName(), "" + httpURLConnection.getResponseCode()); //$NON-NLS-1$

            BufferedReader bufferedReaderInputStream = null;
            try {
                bufferedReaderInputStream = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));

                responseString = new StringBuffer();
                String inputLine;
                while ((inputLine = bufferedReaderInputStream.readLine()) != null) {
                    responseString.append(inputLine);
                }

            } catch(Exception exception) {
                Log.e(HTTPUtil.class.getCanonicalName(), exception.getLocalizedMessage());
            } finally {
                IOUtils.closeQuietly(bufferedReaderInputStream);
            }

        } catch (MalformedURLException malformedURLException) {
            Log.e(HTTPUtil.class.getCanonicalName(), malformedURLException.getLocalizedMessage());
        } catch (IOException ioException) {
            Log.e(HTTPUtil.class.getCanonicalName(), ioException.getLocalizedMessage());
        } catch (NullPointerException nullPointerException) {
            Log.e(HTTPUtil.class.getCanonicalName(), nullPointerException.getLocalizedMessage());
        } finally {
            httpURLConnection.disconnect();

            if(null == responseString || responseString.toString().isEmpty()) {
                return "defaultAPIResponse"; //$NON-NLS-1$
            }

            return responseString.toString();
        }
    }
}
