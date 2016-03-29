package invin.com.similarmovies.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import invin.com.similarmovies.BuildConfig;

/**
 * Utility to read properties from 'assets'
 */
public final class PropertyUtil {

    /**
     * Given the current {@link Context} & the file name, returns the asset's properties.
     *
     * @param applicationContext
     *              The current {@link Context}
     * @param fileName
     *              The name of the file.
     * @return Possibly null {@link Properties}
     */
    public static Properties getProperties(Context applicationContext, String fileName) {
        Properties properties = null;
        try {
            /**
             * getAssets() Return an AssetManager instance for your
             * application's package. AssetManager Provides access to an
             * application's raw asset files;
             */
            AssetManager assetManager = applicationContext.getAssets();

            /**
             * Open an asset using ACCESS_STREAMING mode. This
             */
            InputStream inputStream = assetManager.open(fileName);

            /**
             * Loads properties from the specified InputStream,
             */
            properties.load(inputStream);

        } catch (IOException ioException) {
            Log.e(PropertyUtil.class.getCanonicalName(), ioException.toString());
        }

        return properties;
    }

    /**
     *  Returns the RottenTomatoes API key from the assets folder.
     *  ('Assets' is added through Android Studio, default location (/res/assets))
     *
     * @param applicationContext
     *              The current {@link Context}
     * @return {@link java.lang.String} consisting of the RottenTomatoes API key
     */
    public static String returnRottenTomatoesAPIKeyFromAssets(Context applicationContext) {
        AssetManager assetManager = applicationContext.getAssets();
        BufferedReader bufferedAssetsFileReader = null;
        String propertiesFileName = "key.properties";
        String[] keyValue;

        StringBuilder keyBuilder = new StringBuilder();
        InputStream propertiesReader = null;
        try {
            propertiesReader = assetManager.open(propertiesFileName);
            bufferedAssetsFileReader = new BufferedReader(new InputStreamReader(propertiesReader, "UTF-8"));

            String str;
            while ((str=bufferedAssetsFileReader.readLine()) != null) {
                if(str.contains("RottenTomatoesAPIKey")){
                    keyValue = str.split("=");
                    keyBuilder.append(keyValue[1]);
                }
            }
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            if (BuildConfig.DEBUG) {
                Log.d(PropertyUtil.class.getCanonicalName(), unsupportedEncodingException.getLocalizedMessage());
            }
        } catch (IOException ioException) {
            if (BuildConfig.DEBUG) {
                Log.d(PropertyUtil.class.getCanonicalName(), ioException.getLocalizedMessage());
            }
        } catch (Exception exception) {
            if (BuildConfig.DEBUG) {
                Log.d(PropertyUtil.class.getCanonicalName(), exception.getLocalizedMessage());
            }
        } finally {
            IOUtils.closeQuietly(propertiesReader);
            IOUtils.closeQuietly(bufferedAssetsFileReader);
            return keyBuilder.toString();
        }
    }
}
