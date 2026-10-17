
 import java.sql.Connection;

public class test {
   

    public static void main(String[] args) {
        Connection con = Conexion.conectar();

        if (con != null) {
            System.out.println("Conectado a MySQL correctamente ✔");
        } else {
            System.out.println("No se pudo conectar ❌");
        }
    }
}


