package colectivo.conexion;

import java.util.Hashtable;
import java.util.ResourceBundle;

public class Factory {

    private static Hashtable<String, Object> instancias = new Hashtable<>();

    public static Object getInstancia(String objName) {
        try {
            Object obj = instancias.get(objName);
            if (obj == null) {
                ResourceBundle rb = ResourceBundle.getBundle("factory");
                String sClassname = rb.getString(objName).trim();
                obj = Class.forName(sClassname).getDeclaredConstructor().newInstance();
                instancias.put(objName, obj);
            }
            return obj;
        } catch (Exception ex) {
            throw new RuntimeException("Error creando instancia para: " + objName, ex);
        }
    }

}
