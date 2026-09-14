-- 1. Lieferanten
CREATE TABLE suppliers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    contact_email TEXT,
    address TEXT
);

-- 2. Produkte (Die Definition des Artikels)
CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sku TEXT UNIQUE NOT NULL, -- Die eindeutige Artikelnummer
    name TEXT NOT NULL,
    category TEXT,
    description TEXT,
    min_stock_level INTEGER DEFAULT 10 -- Für FR5: Low-Stock Alert
);

-- 3. Verknüpfung Produkt <-> Lieferant (Many-to-Many)
CREATE TABLE product_suppliers (
    product_id INTEGER,
    supplier_id INTEGER,
    PRIMARY KEY (product_id, supplier_id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- 4. Batches (Hier passiert das FIFO & die Garantie!)
CREATE TABLE stock_batches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,        -- Wie viele Stück in diesem Batch?
    received_date DATE NOT NULL,      -- Für FIFO!
    expiry_date DATE,                 -- Ablaufdatum / Garantie
    location_id INTEGER,              -- Wo liegt dieser spezifische Batch?
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (location_id) REFERENCES locations(id)
);

-- 5. Lagerplätze
CREATE TABLE locations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    location_code TEXT UNIQUE NOT NULL -- z.B. "A-01-12"
);
