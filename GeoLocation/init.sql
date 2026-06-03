CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE devices (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,         -- Identificador único do dispositivo
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
    contact_info VARCHAR(255),
    location GEOMETRY(Point, 4326) NOT NULL, -- Coordenada (Lon, Lat)
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Índice Espacial (Fundamental para performance)
CREATE INDEX idx_devices_location ON devices USING GIST (location);
CREATE INDEX idx_marketplace_items_location ON marketplace_items USING GIST (location);

CREATE TABLE messages (
    message_id VARCHAR(255) PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    chat_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(50) DEFAULT 'text',
    location GEOMETRY(Point, 4326) NOT NULL,   -- Kept the spatial requirement
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_messages_sender ON messages (sender_id);
CREATE INDEX idx_messages_recipient ON messages (recipient_id);
CREATE INDEX idx_messages_chat ON messages (chat_id);
CREATE INDEX idx_messages_location ON messages USING GIST (location);
