CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE devices (
    id SERIAL PRIMARY KEY,
    device_id UUID NOT NULL,                 -- Identificador único do dispositivo
    location GEOMETRY(Point, 4326) NOT NULL, -- Coordenada (Lon, Lat)
    accuracy FLOAT,                          -- Precisão do GPS em metros
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE marketplace_items (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price FLOAT NOT NULL,
    address VARCHAR(255),
    contact_info VARCHAR(255)
);

-- 3. Índice Espacial (Fundamental para performance)
CREATE INDEX idx_devices_location ON devices USING GIST (location);
