package colectivo.aplicacion;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Centraliza la gestión de idioma de la aplicación.
 * Permite cambiar el {@link Locale} en tiempo de ejecución y notificar a
 * los interesados mediante una propiedad observable.
 */
public final class Configuracion {

    private static final String BUNDLE_BASE = "i18n.messages";
    private static final Locale DEFAULT_LOCALE = new Locale("es");
    private static final Configuracion INSTANCE = new Configuracion();

    private final ObjectProperty<Locale> localeProperty = new SimpleObjectProperty<>(DEFAULT_LOCALE);
    private ResourceBundle bundle = loadBundle(DEFAULT_LOCALE);

    private Configuracion() {
        localeProperty.addListener((obs, oldLocale, newLocale) -> bundle = loadBundle(newLocale));
    }

    public static Configuracion getInstance() {
        return INSTANCE;
    }

    public Locale getLocale() {
        return localeProperty.get();
    }

    public void setLocale(Locale locale) {
        if (locale == null) {
            return;
        }
        if (!locale.equals(getLocale())) {
            localeProperty.set(locale);
        }
    }

    public ObjectProperty<Locale> localeProperty() {
        return localeProperty;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    private ResourceBundle loadBundle(Locale locale) {
        Locale effective = (locale == null) ? DEFAULT_LOCALE : locale;
        return ResourceBundle.getBundle(BUNDLE_BASE, effective);
    }
}