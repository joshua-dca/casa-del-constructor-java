
import com.toedter.calendar.JDateChooser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.JOptionPane;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JInternalFrame.java to edit this template
 */

public class RegistrarCompra extends javax.swing.JInternalFrame {

    // Variable para controlar que solo se cree una cabecera de compra por sesión
    private int idCompraGenerado = 0;
    private int idProductoGenerado=0;
   
    public RegistrarCompra() {
        initComponents();
         CargarProveedor();
       
    }

    // Carga los proveedores existentes en el ComboBox
    public void CargarProveedor() {
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement("SELECT nombre FROM proveedores");
             ResultSet rs = ps.executeQuery()) {
            
            Proveedor_Caja.removeAllItems();
            while (rs.next()) {
                Proveedor_Caja.addItem(rs.getString("nombre"));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar proveedores: " + e.getMessage());
        }
    }

    // Paso 1: Crea el registro en la tabla 'compras' (Cabecera)
    public void registrarCabecera() {
        if (idCompraGenerado != 0 && idProductoGenerado !=0) return; // Si ya se generó en este clic, no hace nada

        String proveedor = Proveedor_Caja.getSelectedItem().toString();
        Date fechaDate = Fecha_Compra.getDate();

        if (fechaDate == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una fecha válida.");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fechaStr = sdf.format(fechaDate);

        try {
            Connection con = Conexion.conectar();
            String sql = "INSERT INTO compras (fecha, proveedor) VALUES (?,?)";
            PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, fechaStr);
            ps.setString(2, proveedor);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                idCompraGenerado = rs.getInt(1);
                idProductoGenerado= rs.getInt(1);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al crear registro de compra: " + e.getMessage());
        }
    }

    public void agregarDetalleYStock() {
    if (idCompraGenerado == 0) {
        JOptionPane.showMessageDialog(this, "Primero debe generarse la cabecera de la compra.");
        return;
    }

    String nombreProducto = Producto_compra.getText().trim();
    String cantidadStr = Cantidad_compra.getText().trim();
    String precioStr = Precio_compra.getText().trim();

    if (nombreProducto.isEmpty() || cantidadStr.isEmpty() || precioStr.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Debe llenar Producto, Cantidad y Precio.");
        return;
    }

    try (Connection con = Conexion.conectar()) {
        int cantidadInt = Integer.parseInt(cantidadStr);
        double precioCosto = Double.parseDouble(precioStr);

        // PASO 1: Buscar si el producto existe para obtener su ID real
        int idRealProducto = 0;
        PreparedStatement psBusqueda = con.prepareStatement("SELECT id_producto FROM productos WHERE nombre = ?");
        psBusqueda.setString(1, nombreProducto);
        ResultSet rsB = psBusqueda.executeQuery();

        if (rsB.next()) {
            idRealProducto = rsB.getInt("id_producto");
        } else {
            // PASO 2: Si NO existe, lo creamos en la tabla productos (con stock 0 y precio 0 por ahora)
            PreparedStatement psInsertProd = con.prepareStatement(
                "INSERT INTO productos (nombre, precio, cantidad) VALUES (?, 0, 0)", 
                PreparedStatement.RETURN_GENERATED_KEYS
            );
            psInsertProd.setString(1, nombreProducto);
            psInsertProd.executeUpdate();
            ResultSet rsKey = psInsertProd.getGeneratedKeys();
            if (rsKey.next()) idRealProducto = rsKey.getInt(1);
        }

        // PASO 3: Insertar en detalle_compra usando el ID REAL del producto
        String sqlDetalle = "INSERT INTO detalle_compra (id_compra, id_producto, cantidad, precio_compra) VALUES (?,?,?,?)";
        PreparedStatement psD = con.prepareStatement(sqlDetalle);
        psD.setInt(1, idCompraGenerado);
        psD.setInt(2, idRealProducto); // Aquí usamos el ID que encontramos o creamos
        psD.setInt(3, cantidadInt);
        psD.setDouble(4, precioCosto);
        psD.executeUpdate();

        // PASO 4: Actualizar Stock (Sumar lo comprado)
        PreparedStatement psS = con.prepareStatement("UPDATE productos SET cantidad = cantidad + ? WHERE id_producto = ?");
        psS.setInt(1, cantidadInt);
        psS.setInt(2, idRealProducto);
        psS.executeUpdate();

        JOptionPane.showMessageDialog(this, "¡Producto '" + nombreProducto + "' añadido y Stock actualizado!");

        // Limpiar campos
        Producto_compra.setText("");
        Cantidad_compra.setText("");
        Precio_compra.setText("");

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }
}

    // Método opcional para resetear todo y empezar una nueva factura
    public void nuevaFactura() {
        idCompraGenerado = 0;
        idProductoGenerado=0;
        Producto_compra.setText("");
        Cantidad_compra.setText("");
        Precio_compra.setText("");
        JOptionPane.showMessageDialog(this, "Listo para nueva factura de proveedor.");
    }

    /* El resto del código (initComponents y declaraciones de variables) 
       lo genera automáticamente el diseño de NetBeans */


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        Cantidad_compra = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        Precio_compra = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        Fecha_Compra = new com.toedter.calendar.JDateChooser();
        Proveedor_Caja = new javax.swing.JComboBox<>();
        Producto_compra = new javax.swing.JTextField();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setTitle("Registro Compra");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Fecha");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Proveedor");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Producto");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Cantidad");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Precio");

        jButton2.setBackground(new java.awt.Color(46, 204, 113));
        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setIcon(new javax.swing.ImageIcon("C:\\Users\\da726\\Downloads\\plus (2).png")); // NOI18N
        jButton2.setText("Agregar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        Proveedor_Caja.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Proveedor_Caja, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Fecha_Compra, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Precio_compra, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Producto_compra, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Cantidad_compra, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addComponent(jButton2)))
                .addContainerGap(155, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(Fecha_Compra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(Producto_compra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(Cantidad_compra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(Precio_compra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Proveedor_Caja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(25, 25, 25)
                        .addComponent(jButton2)))
                .addContainerGap(155, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        registrarCabecera();
        agregarDetalleYStock();
    }//GEN-LAST:event_jButton2ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField Cantidad_compra;
    private com.toedter.calendar.JDateChooser Fecha_Compra;
    private javax.swing.JTextField Precio_compra;
    private javax.swing.JTextField Producto_compra;
    private javax.swing.JComboBox<String> Proveedor_Caja;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    // End of variables declaration//GEN-END:variables
}
