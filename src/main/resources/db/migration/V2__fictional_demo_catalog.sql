-- Catálogo íntegramente ficticio. No contiene información de Dentaltix ni de otras empresas.
INSERT INTO products VALUES ('DEMO-001','Kit de instrumental de demostración'),('DEMO-002','Caja de consumibles de demostración'),('DEMO-003','Material de demostración sin ofertas');
INSERT INTO suppliers VALUES (1,'Proveedor Atlas (ficticio)'),(2,'Proveedor Brisa (ficticio)'),(3,'Proveedor Cobalto (ficticio)');
INSERT INTO offers(product_sku,supplier_id,region,unit_price,stock,lead_days) VALUES
 ('DEMO-001',1,'MADRID',12.50,100,3),
 ('DEMO-001',2,'MADRID',14.00,50,1),
 ('DEMO-001',3,'MADRID',11.00,2,5),
 ('DEMO-001',1,'ANDALUCIA',13.00,80,4),
 ('DEMO-001',2,'CATALUNA',14.50,60,2),
 ('DEMO-002',1,'MADRID',8.75,200,2),
 ('DEMO-002',2,'ANDALUCIA',9.00,150,1);
