package invin.com.similarmovies.util;

/**
 * Stores useful constants used throughout the App.
 */
public final class Constants {

    // JSON tags
    public final static String TAG_ID;
    public final static String TAG_TITLE;
    public final static String TAG_MOVIES;

    // Strings to assist in the API URL build
    public final static String API_URL_PREPENDER;
    public final static String API_MOVIE_SEARCH_QUERY_APPENDER;
    public final static String API_URL_SIMILAR_CONNECTOR;
    public final static String API_MOVIE_SEARCH_CONNECTOR;

    public final static String FORWARD_SLASH;

    // Various intents in use
    public final static String INTENT_KEY;
    public final static String INTENT_MOVIE_ID;
    public final static String INTENT_MOVIE_NAME;
    public final static String INTENT_MOVIE_ID_NAME;

    // Search constants
    public final static int SEARCH_BY_MOVIE_NAME;
    public final static int SEARCH_BY_MOVIE_ID;

    static{
        TAG_ID = "id";
        TAG_TITLE = "title";
        TAG_MOVIES = "movies";

        API_URL_PREPENDER = "http://api.rottentomatoes.com/api/public/v1.0/movies";
        API_MOVIE_SEARCH_QUERY_APPENDER = ".json?q=";
        API_MOVIE_SEARCH_CONNECTOR = "&page_limit=10&page=1&apikey=";
        API_URL_SIMILAR_CONNECTOR = "similar.json?apikey=";
        FORWARD_SLASH = "/";

        INTENT_KEY = "com.invin.similarmovies.KEY";
        INTENT_MOVIE_ID = "com.invin.similarmovies.MOVIE_ID";
        INTENT_MOVIE_NAME = "com.invin.similarmovies.MOVIE_NAME";
        INTENT_MOVIE_ID_NAME = "com.invin.similarmovies.MOVIE_ID_NAME";

        SEARCH_BY_MOVIE_NAME = 1;
        SEARCH_BY_MOVIE_ID = 2;
    }
}
