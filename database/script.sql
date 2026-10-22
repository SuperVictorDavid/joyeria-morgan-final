-- Joyeria Morgan - script base MySQL 8.0
-- BD: joyeria_db (creada por docker createDatabaseIfNotExist + JPA ddl-auto:update)
-- Tablas principales generadas por JPA: joyas, ventas, app_users, customers, secure_sales
-- Seed de referencia (JPA DataInitializer crea lo mismo si tablas vacias):

-- Usuarios (password BCrypt de admin123 / vendedor123, generar en backend):
-- INSERT INTO app_users(username,email,password,role) VALUES
-- ('admin','admin@joyeriamorgan.pe','$2a$10$...','ADMIN'),
-- ('vendedor','vendedor@joyeriamorgan.pe','$2a$10$...','VENDEDOR');

-- Joyas ejemplo:
-- INSERT INTO joyas(name,descripcion,tipo_producto,tipo_material,quilates,peso,price,stock,estado) VALUES
-- ('Anillo de Oro 18K','Anillo compromiso oro 18 quilates','Anillo','Oro',18.0,5.20,2500.00,10,'Disponible'),
-- ('Cadena de Plata 925','Cadena tipo collar plata 925','Cadena','Plata',0.0,12.50,850.00,25,'Disponible'),
-- ('Pulsera de Platino','Pulsera elegante platino','Pulsera','Platino',0.0,8.00,3200.00,5,'Disponible');

-- Ventas ejemplo:
-- INSERT INTO ventas(numero_boleta,product_id,quantity,customer_id,vendedor,total,status) VALUES
-- ('B001-000001',1,2,9001,'vendedor',5000.00,'COMPLETED'),
-- ('B001-000002',2,1,9002,'vendedor',850.00,'COMPLETED');

-- Verificacion AP1 (pruebas DAO):
-- SELECT COUNT(*) FROM joyas;
-- SELECT COUNT(*) FROM ventas;
-- SELECT username, role FROM app_users;
