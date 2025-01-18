package com.example.vista;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";
    private static final String SELECTED_COUNTRY = "Locale.Helper.Selected.Country";

    /**
     * Set the locale and return the updated context.
     *
     * @param context     The current context.
     * @param language    The language code (e.g., "en", "zh").
     * @param country     The country code (e.g., "US", "HK").
     * @return Context with the updated locale.
     */
    public static Context setLocale(Context context, String languageCode, String countryCode) {
        Locale locale = new Locale(languageCode, countryCode);
        Locale.setDefault(locale);

        // Apply the locale to the context
        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);

        // Update the resources with the new locale
        context = context.createConfigurationContext(config);

        return context;
    }

    /**
     * Retrieve the current language from SharedPreferences.
     *
     * @param context The current context.
     * @return The language code.
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        return prefs.getString(SELECTED_LANGUAGE, Locale.getDefault().getLanguage());
    }

    /**
     * Retrieve the current country from SharedPreferences.
     *
     * @param context The current context.
     * @return The country code.
     */
    public static String getCountry(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        return prefs.getString(SELECTED_COUNTRY, Locale.getDefault().getCountry());
    }

    /**
     * Persist the language and country to SharedPreferences.
     *
     * @param context  The current context.
     * @param language The language code.
     * @param country  The country code.
     */
    private static void persist(Context context, String language, String country) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        prefs.edit()
                .putString(SELECTED_LANGUAGE, language)
                .putString(SELECTED_COUNTRY, country)
                .apply();
    }

    /**
     * Update the app's resources to the new locale.
     *
     * @param context  The current context.
     * @param language The language code.
     * @param country  The country code.
     * @return The context with updated resources.
     */
    private static Context updateResources(Context context, String language, String country) {
        Locale locale = new Locale(language, country);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    /**
     * Apply the locale when attaching the base context.
     *
     * @param context The current context.
     * @return The context with the applied locale.
     */
    public static Context onAttach(Context context) {
        String lang = getLanguage(context);
        String country = getCountry(context);
        return setLocale(context, lang, country);
    }
}