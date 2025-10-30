package colectivo.conexion;

import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Factory {

    private static Hashtable<String, Object> instancias = new Hashtable<>();

    public static Object getInstancia(String objName) {
        try {
            // verifico si existe un objeto relacionado a objName
            // en la hashtable
            Object obj = instancias.get(objName);
            // si no existe entonces lo instancio y lo agrego
            if (obj == null) {
                ResourceBundle rb = ResourceBundle.getBundle("factory");
                // trim() evita problemas por espacios en el properties
                String sClassname = rb.getString(objName).trim();
                obj = Class.forName(sClassname).getDeclaredConstructor().newInstance();
                // agrego el objeto a la hashtable
                instancias.put(objName, obj);
            }
            return obj;
        } catch (Exception ex) {
            // No imprimir stack trace aquí: lanzar con causa y mensaje claro.
            throw new RuntimeException("Error creando instancia para: " + objName, ex);
        }
    }
}
