
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import javax.swing.JOptionPane;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.toedter.calendar.JDateChooser;

public class RegistrarVenta extends javax.swing.JInternalFrame {

int idVentaGenerada;
JDateChooser fechaChooser = new JDateChooser();

public RegistrarVenta() {
        initComponents();

        // Tabla vacía
        DefaultTableModel modelo = (DefaultTableModel) Tabla_producto.getModel();
        modelo.setRowCount(0);

        // Agregar calendario
        
        fechaChooser.setDate(new Date());

        CargarCliente();
        CargarProducto();
    }
   public void CrearVenta(){
   
   String cliente = Cliente_caja.getSelectedItem().toString();
   Date fechaDate = jDateChooser1.getDate();

if(fechaDate == null){
    JOptionPane.showMessageDialog(this,"Selecciona una fecha");
    return;
}

SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
String fecha = sdf.format(fechaDate);

    try{
        Connection con = Conexion.conectar();

        PreparedStatement psCliente = con.prepareStatement(
            "SELECT id_cliente FROM clientes WHERE nombre_cliente=?"
        );
        psCliente.setString(1, cliente);
        ResultSet rs = psCliente.executeQuery();

        int idCliente = 0;
        if(rs.next()){
            idCliente = rs.getInt("id_cliente");
        }

        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO ventas (Fechas, id_cliente) VALUES (?,?)",
            PreparedStatement.RETURN_GENERATED_KEYS
        );

        ps.setString(1, fecha);
        ps.setInt(2, idCliente);
        ps.executeUpdate();

        ResultSet rs2 = ps.getGeneratedKeys();
        if(rs2.next()){
            idVentaGenerada = rs2.getInt(1);
        }

        JOptionPane.showMessageDialog(this,"Venta creada");

    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error: "+e.getMessage());
    }
   } 
    public void CargarCliente(){
   try{
        Connection con = Conexion.conectar();
        PreparedStatement ps = con.prepareStatement("SELECT nombre_cliente FROM clientes");
        ResultSet rs = ps.executeQuery();

        Cliente_caja.removeAllItems();

        while(rs.next()){
            Cliente_caja.addItem(rs.getString("nombre_cliente"));
        }

        con.close();
    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error: "+e.getMessage());
  }
    }
   public void CargarProducto(){
   try{
        Connection con = Conexion.conectar();
        PreparedStatement ps = con.prepareStatement("SELECT nombre FROM productos");
        ResultSet rs = ps.executeQuery();

        Producto_caja.removeAllItems();

        while(rs.next()){
            Producto_caja.addItem(rs.getString("nombre"));
        }

        con.close();
    }catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error: "+e.getMessage());
  }
   
   }
 public void agregraProducto() {
    if (Cantidad_venta.getText().isEmpty() || Precio_venta.getText().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Llena cantidad y precio");
        return;
    }

    String producto = Producto_caja.getSelectedItem().toString();
    int cantidadSolicitada = Integer.parseInt(Cantidad_venta.getText());
    
    try (Connection con = Conexion.conectar()) {
        // Consultamos el stock real en este momento
        PreparedStatement ps = con.prepareStatement("SELECT cantidad FROM productos WHERE nombre = ?");
        ps.setString(1, producto);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int stockActual = rs.getInt("cantidad");

            // VALIDACIÓN CRÍTICA
            if (stockActual <= 0) {
                JOptionPane.showMessageDialog(this, "No puedes agregar este producto. El stock es 0.");
                return; // Detiene el método aquí
            }
            
            if (cantidadSolicitada > stockActual) {
                JOptionPane.showMessageDialog(this, "No hay suficiente stock. Disponible: " + stockActual);
                return; // Detiene el método aquí
            }

            // Si pasa las validaciones, procedemos a agregar a la tabla
            DefaultTableModel modelo = (DefaultTableModel) Tabla_producto.getModel();
            double precio = Double.parseDouble(Precio_venta.getText());
            double subtotal = cantidadSolicitada * precio;

            modelo.addRow(new Object[]{producto, cantidadSolicitada, precio, subtotal});

            // Limpiar
            Cantidad_venta.setText("");
            Precio_venta.setText("");
            calcularTotal();
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al validar stock: " + e.getMessage());
    }
}
 public void RegistrarVentaCompleta() {
    if (Tabla_producto.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "Agrega productos primero");
        return;
    }

    // Paso 1: Crea la cabecera de la venta (idVentaGenerada)
    CrearVenta(); 

    try {
        Connection con = Conexion.conectar();
        DefaultTableModel modelo = (DefaultTableModel) Tabla_producto.getModel();

        for (int i = 0; i < modelo.getRowCount(); i++) {
            String producto = modelo.getValueAt(i, 0).toString();
            int cantidadAVender = Integer.parseInt(modelo.getValueAt(i, 1).toString());
            double precio = Double.parseDouble(modelo.getValueAt(i, 2).toString());

            // 1. Obtener el ID del producto
            PreparedStatement psProd = con.prepareStatement(
                "SELECT id_producto FROM productos WHERE nombre=?"
            );
            psProd.setString(1, producto);
            ResultSet rs = psProd.executeQuery();

            int idProducto = 0;
            if (rs.next()) {
                idProducto = rs.getInt("id_producto");
            }

            // 2. Insertar en detalle_ventas
            PreparedStatement psDetalle = con.prepareStatement(
                "INSERT INTO detalle_ventas (id_ventas, id_producto, cantidad, precio_unitario) VALUES (?,?,?,?)"
            );
            psDetalle.setInt(1, idVentaGenerada);
            psDetalle.setInt(2, idProducto);
            psDetalle.setInt(3, cantidadAVender);
            psDetalle.setDouble(4, precio);
            psDetalle.executeUpdate();

            // 3. DESCONTAR DEL STOCK (Actualización del inventario)
            // Restamos la cantidad que el cliente se está llevando
            PreparedStatement psStock = con.prepareStatement(
                "UPDATE productos SET cantidad = cantidad - ? WHERE id_producto = ?"
            );
            psStock.setInt(1, cantidadAVender);
            psStock.setInt(2, idProducto);
            psStock.executeUpdate();
        }

        JOptionPane.showMessageDialog(this, "Venta registrada e inventario actualizado correctamente");

        // Limpiar tabla y actualizar etiqueta de total
      
        modelo.setRowCount(0);
        calcularTotal();

        con.close();

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al procesar la venta: " + e.getMessage());
    }
}
  public void calcularTotal(){
    double total = 0;
    DefaultTableModel modelo = (DefaultTableModel) Tabla_producto.getModel();

    for(int i = 0; i < modelo.getRowCount(); i++){
        total += Double.parseDouble(modelo.getValueAt(i, 3).toString());
    }

    jLabel6.setText("Total: $" + total);
}
 public void RegistrarVenta() {
        initComponents();
         DefaultTableModel modelo = (DefaultTableModel) Tabla_producto.getModel();
    modelo.setRowCount(0);
        CargarCliente();
        CargarProducto();
    }
public void cargarPrecio(String nombreProducto) {
    // Agregamos 'stock' a la consulta
    String sql = "SELECT precio, cantidad FROM productos WHERE nombre = ?";
    
    try (Connection con = Conexion.conectar();
         PreparedStatement ps = con.prepareStatement(sql)) {
        
        ps.setString(1, nombreProducto);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            double precio = rs.getDouble("precio");
            int stockActual = rs.getInt("cantidad"); // Obtenemos el stock
            
            Precio_venta.setText(String.valueOf(precio));

            // ALERTA PREVENTIVA
            if (stockActual <= 0) {
                JOptionPane.showMessageDialog(this, "¡ATENCIÓN! El producto '" + nombreProducto + "' no tiene stock disponible.", "Sin existencias", JOptionPane.WARNING_MESSAGE);
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al cargar datos del producto: " + e.getMessage());
    }
}
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        Cliente_caja = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        Producto_caja = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        Cantidad_venta = new javax.swing.JTextField();
        Precio_venta = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        Tabla_producto = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();

        jScrollPane1.setViewportView(jTextPane1);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setTitle("Registrar Venta");

        jButton1.setBackground(new java.awt.Color(46, 204, 113));
        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setIcon(new javax.swing.ImageIcon("C:\\Users\\da726\\Downloads\\add-to-basket.png")); // NOI18N
        jButton1.setText("Registar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Cliente");

        Cliente_caja.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Cliente_caja.setToolTipText("");
        Cliente_caja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Cliente_cajaActionPerformed(evt);
            }
        });

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Fecha");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Producto");

        Producto_caja.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Producto_caja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Producto_cajaActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Cantidad");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Precio");

        Cantidad_venta.setForeground(new java.awt.Color(255, 255, 255));

        Tabla_producto.setForeground(new java.awt.Color(255, 255, 255));
        Tabla_producto.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Producto", "Cantidad", "Precio", "Subtotal"
            }
        ));
        jScrollPane3.setViewportView(Tabla_producto);

        jLabel6.setText("Total");

        jButton2.setBackground(new java.awt.Color(46, 204, 113));
        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setIcon(new javax.swing.ImageIcon("C:\\Users\\da726\\Downloads\\add (3).png")); // NOI18N
        jButton2.setText("Agregar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jDateChooser1.setBackground(new java.awt.Color(255, 255, 255));
        jDateChooser1.setForeground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Precio_venta, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Producto_caja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Cliente_caja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Cantidad_venta, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton1))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addComponent(jLabel6)
                                .addGap(178, 178, 178)
                                .addComponent(jButton2)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(Cliente_caja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jButton1))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(Producto_caja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(Cantidad_venta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(Precio_venta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jButton2))
                .addContainerGap(147, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
       agregraProducto();
       
    }//GEN-LAST:event_jButton1ActionPerformed

    private void Cliente_cajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Cliente_cajaActionPerformed
           
    }//GEN-LAST:event_Cliente_cajaActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        RegistrarVentaCompleta();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void Producto_cajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Producto_cajaActionPerformed
        if (Producto_caja.getSelectedItem() != null) {
        String producto = Producto_caja.getSelectedItem().toString();
        cargarPrecio(producto);
    } else {
        // Opcional: limpiar el precio si no hay nada seleccionado
        Precio_venta.setText("");
    }
    }//GEN-LAST:event_Producto_cajaActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField Cantidad_venta;
    private javax.swing.JComboBox<String> Cliente_caja;
    private javax.swing.JTextField Precio_venta;
    private javax.swing.JComboBox<String> Producto_caja;
    private javax.swing.JTable Tabla_producto;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
