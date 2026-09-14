/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.global;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Formatter
{
    public static final int DATE_LONG = 1;
    public static final int DATE_SHORT = 0;
    public static final int DATETIME_LONG = 5;
    public static final int DATETIME_SHORT = 4;
    public static final int TIME_LONG = 3;
    public static final int TIME_SHORT = 2;

    private Locale locale;
    private static final Map<String, Locale> supportedLocales = new HashMap<String, Locale>();

    static
    {
        // Populate the list of supported locales with a few most common ones

        supportedLocales.put("en-US", Locale.US); // English (United States)
        supportedLocales.put("en-UK", Locale.UK); // English (United Kingdom)
        supportedLocales.put("fr-FR", Locale.FRANCE); // French (France)
        supportedLocales.put("de-DE", Locale.GERMANY); // German (Germany)
        supportedLocales.put("es-ES", new Locale("es", "ES")); // Spanish (Spain)
        supportedLocales.put("it-IT", Locale.ITALY); // Italian (Italy)
        supportedLocales.put("ja-JP", Locale.JAPAN); // Japanese (Japan)
        supportedLocales.put("zh-CN", Locale.SIMPLIFIED_CHINESE); // Chinese (Simplified)
        supportedLocales.put("zh-TW", Locale.TRADITIONAL_CHINESE); // Chinese (Traditional)
        supportedLocales.put("ko-KR", Locale.KOREA); // Korean (South Korea)
        supportedLocales.put("pt-PT", new Locale("pt", "PT")); // Portuguese (Portugal)
        supportedLocales.put("pt-BR", new Locale("pt", "BR")); // Portuguese (Brazil)
        supportedLocales.put("ru-RU", new Locale("ru", "RU")); // Russian (Russia)
        supportedLocales.put("ar-AE", new Locale("ar", "AE")); // Arabic (UAE)
        supportedLocales.put("hi-IN", new Locale("hi", "IN")); // Hindi (India)
        supportedLocales.put("tr-TR", new Locale("tr", "TR")); // Turkish (Turkey)
    }

    public Formatter()
    {
        String systemLocale = System.getProperty("microedition.locale");
        supportedLocales.get(systemLocale);
    }

    public Formatter(String locale)
    {
        if (locale == null || locale.length() == 0) { this.locale = Locale.getDefault(); }
        else
        {
            if (!isLocaleSupported(locale)) { throw new UnsupportedLocaleException("Unsupported locale: " + locale); }

            this.locale = supportedLocales.get(locale);
        }
    }

    public String formatCurrency(double number)
    {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        return formatter.format(number);
    }

    public String formatDateTime(Calendar dateTime, int style)
    {
        if (dateTime == null) { throw new NullPointerException("dateTime cannot be null"); }

        SimpleDateFormat sdf;
        switch (style)
        {
            case DATE_LONG:
                sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", locale);
                break;
            case DATE_SHORT:
                sdf = new SimpleDateFormat("MM/dd/yyyy", locale);
                break;
            case DATETIME_LONG:
                sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", locale);
                break;
            case DATETIME_SHORT:
                sdf = new SimpleDateFormat("MM/dd/yyyy h:mm a", locale);
                break;
            case TIME_LONG:
                sdf = new SimpleDateFormat("h:mm:ss a", locale);
                break;
            case TIME_SHORT:
                sdf = new SimpleDateFormat("h:mm a", locale);
                break;
            default:
                throw new IllegalArgumentException("Invalid style");
        }
        return sdf.format(dateTime.getTime());
    }

    public String formatNumber(double number)
    {
        NumberFormat formatter = NumberFormat.getInstance(locale);
        return formatter.format(number);
    }

    public String formatPercentage(float value, int decimals)
    {
        if (decimals < 1 || decimals > 15) { throw new IllegalArgumentException("Decimals must be between 1 and 15"); }

        NumberFormat percentFormatter = NumberFormat.getPercentInstance(locale);
        percentFormatter.setMinimumFractionDigits(decimals);
        percentFormatter.setMaximumFractionDigits(decimals);
        return percentFormatter.format(value);
    }

    public String getLocale() { return locale.toString(); }

    public static String[] getSupportedLocales()
    {
        Set<String> keys = supportedLocales.keySet();
        return keys.toArray(new String[0]);
    }

    public static String formatMessage(String template, String[] params)
    {
        for (int i = 0; i < params.length; i++) {
            template = template.replace("{" + i + "}", params[i]);
        }
        return template;
    }

    public static boolean isLocaleSupported(String locale)
    {
        if (supportedLocales.containsKey(locale)) { return true; }

        return false;
    }
}
