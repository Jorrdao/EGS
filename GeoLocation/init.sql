CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE geo_resources (
    id SERIAL PRIMARY KEY,
    device_id UUID NOT NULL,                 -- Identificador único do dispositivo
    location GEOMETRY(Point, 4326) NOT NULL, -- Coordenada (Lon, Lat)
    accuracy FLOAT,                          -- Precisão do GPS em metros
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Índice Espacial (Fundamental para performance)
CREATE INDEX idx_geo_resources_location ON geo_resources USING GIST (location);