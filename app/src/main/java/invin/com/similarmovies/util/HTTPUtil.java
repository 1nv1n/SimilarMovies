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

//        HttpClient defaultHTTPClient = new DefaultHttpClient();
//        String apiResponse = "defaultAPIResponse";
//
//        HttpGet movieSearchHTTPRequest = new HttpGet(
//                API_URL_PREPENDER +
//                API_MOVIE_SEARCH_QUERY_APPENDER +
//                movieToSearchFor +
//                API_MOVIE_SEARCH_CONNECTOR +
//                apiKey);
//
//        HttpResponse movieSearchHTTPResponse;
//
//        InputStreamReader apiResponseInputStreamReader = null;
//        BufferedReader inputStreamBufferedReader = null;
//        InputStream inputStreamFromAPIResponse = null;
//
//        try {
//            movieSearchHTTPResponse = defaultHTTPClient.execute(movieSearchHTTPRequest);
//            //As long as we don't get a null response, continue processing
//            if(!movieSearchHTTPResponse.equals(null)){
//                inputStreamFromAPIResponse = movieSearchHTTPResponse.getEntity().getContent();
//
//                if ("gzip".equals(movieSearchHTTPResponse.getEntity().getContentEncoding())){
//                    inputStreamFromAPIResponse = new GZIPInputStream(inputStreamFromAPIResponse);
//                }
//
//                apiResponseInputStreamReader = new InputStreamReader(inputStreamFromAPIResponse);
//                inputStreamBufferedReader = new BufferedReader(apiResponseInputStreamReader);
//
//                StringBuilder inputStreamStringBuilder = new StringBuilder();
//                String inputRead = inputStreamBufferedReader.readLine();
//
//                while(inputRead != null) {
//                    inputStreamStringBuilder.append(inputRead);
//                    inputRead = inputStreamBufferedReader.readLine();
//                }
//
//                apiResponse = inputStreamStringBuilder.toString();
//            }
//        } catch (ClientProtocolException e) {
//            Log.d(HomeScreenActivity.class.getCanonicalName(), "ClientProtocolException:" + e.toString());
//        } catch (IOException ioException) {
//            Log.d(HomeScreenActivity.class.getCanonicalName(), ioException.getLocalizedMessage());
//        } catch (Exception exception) {
//            Log.d(HomeScreenActivity.class.getCanonicalName(), exception.getLocalizedMessage());
//        } finally {
//            IOUtils.closeQuietly(inputStreamFromAPIResponse);
//            IOUtils.closeQuietly(apiResponseInputStreamReader);
//            IOUtils.closeQuietly(inputStreamBufferedReader);
//            return apiResponse;
//        }



//        HttpClient defaultHTTPClient = new DefaultHttpClient();
//
//        String apiURLPrepender = "http://api.rottentomatoes.com/api/public/v1.0/movies";
//        String apiURLSimilarConnector = "similar.json?apikey=";
//        String forwardSlash = "/";
//        String apiResponse = "defaultAPIResponse";
//
//        HttpGet similarMoviesHTTPRequest = new HttpGet(
//                apiURLPrepender +
//                forwardSlash +
//                movieID +
//                forwardSlash +
//                apiURLSimilarConnector +
//                apiKey);
//
//        HttpResponse similarMoviesHTTPResponse;
//
//        InputStream inputStreamFromAPIResponse = null;
//        BufferedReader inputStreamBufferedReader = null;
//        InputStreamReader apiResponseInputStreamReader = null;
//
//        try {
//            similarMoviesHTTPResponse = defaultHTTPClient.execute(similarMoviesHTTPRequest);
//            // As long as we don't get a null response, continue processing
//            if(!similarMoviesHTTPResponse.equals(null)){
//                inputStreamFromAPIResponse = similarMoviesHTTPResponse.getEntity().getContent();
//
//                if ("gzip".equals(similarMoviesHTTPResponse.getEntity().getContentEncoding())){
//                    inputStreamFromAPIResponse = new GZIPInputStream(inputStreamFromAPIResponse);
//                }
//
//                apiResponseInputStreamReader = new InputStreamReader(inputStreamFromAPIResponse);
//                inputStreamBufferedReader = new BufferedReader(apiResponseInputStreamReader);
//
//                StringBuilder inputStreamStringBuilder = new StringBuilder();
//                String inputRead = inputStreamBufferedReader.readLine();
//
//                while(inputRead != null) {
//                    inputStreamStringBuilder.append(inputRead);
//                    inputRead = inputStreamBufferedReader.readLine();
//                }
//
//                apiResponse = inputStreamStringBuilder.toString();
//            }
//        } catch (ClientProtocolException e) {
//            if (BuildConfig.DEBUG) {
//                Log.d(Constants.LOG, "ClientProtocolException"+e.toString());
//            }
//        } catch (IOException e) {
//            if (BuildConfig.DEBUG) {
//                Log.d(Constants.LOG, "IOException"+e.toString());
//            }
//        } catch (Exception e) {
//            if (BuildConfig.DEBUG) {
//                Log.d(Constants.LOG, "Exception"+e.toString());
//            }
//        }
//        finally {
//            IOUtils.closeQuietly(inputStreamBufferedReader);
//            IOUtils.closeQuietly(inputStreamFromAPIResponse);
//            IOUtils.closeQuietly(apiResponseInputStreamReader);
//            return apiResponse;
//        }


    }
}
