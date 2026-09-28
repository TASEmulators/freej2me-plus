/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILTY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package net.rim.device.api.i18n;

import org.recompile.mobile.Mobile;

// Docs say it's compatible with java.util.Locale, so let's rely on that.
public final class Locale
{
	public static final int APPLICATION = 1;
	public static final long GUID_INPUT_LOCALE_CHANGED = -8040378802380461050L;
	public static final long GUID_LOCALE_CHANGED = -7464003439710973532L;
	public static final int KEYBOARD_ID_QWERTY = 0;
	public static final int KEYBOARD_ID_QWERTY_LEGACY = 1295594807;
	public static final int KEYBOARD_ID_QWERTY_PHONE = 1179602501;
	public static final int KEYBOARD_ID_QWERTY_REDUCED = 1364346180;
	public static final int LOCALE_ROOT = 0;
	public static final int LOCALE_SET_DECLINED = 0;
	public static final int LOCALE_SET_OK = 1;
	public static final int LOCALE_af = 1634074624;
	public static final int LOCALE_ar = 1634861056;
	public static final int LOCALE_ca = 1667301376;
	public static final int LOCALE_cs = 1668481024;
	public static final int LOCALE_da = 1684078592;
	public static final int LOCALE_de = 1684340736;
	public static final int LOCALE_el = 1701576704;
	public static final int LOCALE_en = 1701707776;
	public static final int LOCALE_en_GB = 1701726018;
	public static final int LOCALE_en_US = 1701729619;
	public static final int LOCALE_es = 1702035456;
	public static final int LOCALE_es_MX = 1702055256;
	public static final int LOCALE_fa = 1717633024;
	public static final int LOCALE_fr = 1718747136;
	public static final int LOCALE_fr_CA = 1718764353;
	public static final int LOCALE_he = 1751449600;
	public static final int LOCALE_hi = 1751711744;
	public static final int LOCALE_hr = 1752301568;
	public static final int LOCALE_hu = 1752498176;
	public static final int LOCALE_it = 1769209856;
	public static final int LOCALE_ja = 1784741888;
	public static final int LOCALE_ko = 1802436608;
	public static final int LOCALE_nl = 1852571648;
	public static final int LOCALE_no = 1852768256;
	public static final int LOCALE_pl = 1886126080;
	public static final int LOCALE_pt = 1886650368;
	public static final int LOCALE_pt_BR = 1886667346;
	public static final int LOCALE_ro = 1919877120;
	public static final int LOCALE_ru = 1920270336;
	public static final int LOCALE_sv = 1937113088;
	public static final int LOCALE_th = 1952972800;
	public static final int LOCALE_tr = 1953628160;
	public static final int LOCALE_vi = 1986592768;
	public static final int LOCALE_zh = 2053636096;
	public static final int LOCALE_zh_CN = 2053653326;
	public static final int LOCALE_zh_HK = 2053654603;
	public static final int LOCALE_zh_TW = 2053657687;

	private final java.util.Locale innerLoc;

	public Locale(String language)
	{
		this.innerLoc = new java.util.Locale(language);
	}

	public Locale(String language, String country)
	{
		this.innerLoc = new java.util.Locale(language, country);
	}

	public Locale(String language, String country, String variant)
	{
		this.innerLoc = new java.util.Locale(language, country, variant);
	}

	private Locale(java.util.Locale innerLoc) { this.innerLoc = innerLoc; }

	public boolean equals(Object obj)
	{
		Mobile.log(Mobile.LOG_WARNING, Locale.class.getPackage().getName() + "." + Locale.class.getSimpleName() + ": " + "equals not fully implemented.");
		if (obj instanceof Locale)
		{
			return this.innerLoc.equals(((Locale) obj).innerLoc);
		}
		
		return false;
	}

	public static Locale get(int code)
	{
		return get(code, "");
	}

	public static Locale get(int code, String variant)
	{        
		String language = "";
		String country = "";

		switch (code)
		{
			case LOCALE_ROOT:
				language = "";
				country = "";
				break;
			case LOCALE_af:
				language = "af";
				break;
			case LOCALE_ar:
				language = "ar";
				break;
			case LOCALE_ca:
				language = "ca";
				break;
			case LOCALE_cs:
				language = "cs";
				break;
			case LOCALE_da:
				language = "da";
				break;
			case LOCALE_de:
				language = "de";
				break;
			case LOCALE_el:
				language = "el";
				break;
			case LOCALE_en:
				language = "en";
				break;
			case LOCALE_en_GB:
				language = "en";
				country = "GB";
				break;
			case LOCALE_en_US:
				language = "en";
				country = "US";
				break;
			case LOCALE_es:
				language = "es";
				break;
			case LOCALE_es_MX:
				language = "es";
				country = "MX";
				break;
			case LOCALE_fa:
				language = "fa";
				break;
			case LOCALE_fr:
				language = "fr";
				break;
			case LOCALE_fr_CA:
				language = "fr";
				country = "CA";
				break;
			case LOCALE_he:
				language = "he";
				break;
			case LOCALE_hi:
				language = "hi";
				break;
			case LOCALE_hr:
				language = "hr";
				break;
			case LOCALE_hu:
				language = "hu";
				break;
			case LOCALE_it:
				language = "it";
				break;
			case LOCALE_ja:
				language = "ja";
				break;
			case LOCALE_ko:
				language = "ko";
				break;
			case LOCALE_nl:
				language = "nl";
				break;
			case LOCALE_no:
				language = "no";
				break;
			case LOCALE_pl:
				language = "pl";
				break;
			case LOCALE_pt:
				language = "pt";
				break;
			case LOCALE_pt_BR:
				language = "pt";
				country = "BR";
				break;
			case LOCALE_ro:
				language = "ro";
				break;
			case LOCALE_ru:
				language = "ru";
				break;
			case LOCALE_sv:
				language = "sv";
				break;
			case LOCALE_th:
				language = "th";
				break;
			case LOCALE_tr:
				language = "tr";
				break;
			case LOCALE_vi:
				language = "vi";
				break;
			case LOCALE_zh:
				language = "zh";
				break;
			case LOCALE_zh_CN:
				language = "zh";
				country = "CN";
				break;
			case LOCALE_zh_HK:
				language = "zh";
				country = "HK";
				break;
			case LOCALE_zh_TW:
				language = "zh";
				country = "TW";
				break;
			default:
				throw new IllegalArgumentException("Invalid locale");
		}

		if (variant == null) {
			variant = "";
		}

		return new Locale(language, country, variant);
	}

	public static Locale get(String language) { return new Locale(language); }

	public static Locale get(String language, String country)
	{
		return new Locale(language, country);
	}

	public static Locale get(String language, String country, String variant)
	{
		return new Locale(language, country, variant);
	}

	public static Locale[] getAvailableInputLocales()
	{
		return getAvailableLocales();
	}

	public static Locale[] getAvailableLocales()
	{
		java.util.Locale[] locs = java.util.Locale.getAvailableLocales();
		Locale[] locales = new Locale[locs.length];
		for (int i = 0; i < locs.length; i++)
		{
			locales[i] = new Locale(locs[i]);
		}
		return locales;
	}

	public int getCode()
	{
		Mobile.log(Mobile.LOG_WARNING, Locale.class.getPackage().getName() + "." + Locale.class.getSimpleName() + ": " + "getCode not implemented.");
		return 0;
	}

	public String getCountry()
	{
		return this.innerLoc.getCountry();
	}

	public static Locale getDefault()
	{
		return new Locale(java.util.Locale.getDefault());
	}

	public static Locale getDefaultForKeyboard() { return getDefault(); }

	public static Locale getDefaultForSystem() { return getDefault(); }

	public static Locale getDefaultInput() { return getDefault(); }

	public static Locale getDefaultInputForSystem() { return getDefault(); }

	public String getDisplayCountry()
	{
		return this.innerLoc.getDisplayCountry();
	}

	public String getDisplayLanguage()
	{
		return this.innerLoc.getDisplayLanguage();
	}

	public String getDisplayName()
	{
		return this.innerLoc.getDisplayName();
	}

	public String getDisplayVariant()
	{
		return this.innerLoc.getDisplayVariant();
	}

	public static String[] getISOCountries()
	{
		return java.util.Locale.getISOCountries();
	}

	public static String[] getISOLanguages()
	{
		return java.util.Locale.getISOLanguages();
	}

	public String getLanguage() { return this.innerLoc.getLanguage(); }

	public String getVariant() { return this.innerLoc.getVariant(); }

	public int hashCode() { return this.innerLoc.hashCode(); }

	public static void setDefault(Locale defaultLocale)
	{
		if (defaultLocale != null)
		{
			java.util.Locale.setDefault(defaultLocale.innerLoc);
		}
	}

	public static void setDefaultInput(Locale defaultLocale)
	{
		setDefault(defaultLocale);
	}

	public static int setDefaultInputForSystem(Locale locale)
	{
		setDefault(locale);
		return LOCALE_SET_OK;
	}

	public String toString()
	{
		return this.innerLoc.toString();
	}
}
