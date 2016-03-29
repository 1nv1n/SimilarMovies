package invin.com.similarmovies.util;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import invin.com.similarmovies.R;
import invin.com.similarmovies.activity.AboutActivity;

/**
 * Utility to redirect 'on-click's of options
 */
public final class OptionSelectionResolver {

    /**
     * Handle the 'Settings' action from the Action Bar
     */
    public static void openActionSettings(Context applicationContext){
        Toast.makeText(applicationContext, R.string.text_settings_disabled, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle the 'About' action from the Action Bar
     */
    public static void openActionAbout(Context applicationContext){
        Intent intentToShowAboutActivity = new Intent(applicationContext, AboutActivity.class);
        applicationContext.startActivity(intentToShowAboutActivity);
    }
}
