from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import psycopg2
from psycopg2.extras import RealDictCursor

app = FastAPI()

# Conector à Base de Dados (Docker)
def get_db_connection():
    return psycopg2.connect(
        host="localhost", 
        database="geodb", 
        user="user_geo", 
        password="password_geo"
    )

# Modelo de dados que a API espera receber (JSON)
class LocationUpdate(BaseModel):
    device_id: str
    latitude: float
    longitude: float
    accuracy: float

class MarketplaceItem(BaseModel):
    id: int
    name: str
    description: str
    price: float
    address: str
    contact_info: str


@app.post("/api/v1/sync")
#Endpoint para sincronização da cache local ----> bd
async def sync_data():
    pass

@app.get("/api/v1/sync")
#Endpoint para sincronização da cache local <---- bd
async def get_sync_data():
    pass

@app.post("/api/v1/marketplace/items")
async def create_marketplace_item(item: MarketplaceItem):
    try:
        conn = get_db_connection()
        cur = conn.cursor()

        insert_query = """
        INSERT INTO marketplace_items (id, name, price, description, address, contact_info)
        VALUES (%s, %s, %s, %s, %s, %s)
        """
        cur.execute(insert_query, (
            item.id,
            item.name,
            item.price,
            item.description,
            item.address,
            item.contact_info
        ))

        conn.commit()
        cur.close()
        conn.close()
        return {"message": "Item criado com sucesso!"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
@app.get("/api/v1/marketplace/items")
#Endpoint para listar todos os itens do marketplace, filtrar por localização e outros critérios 
async def list_nearby_marketplace_items():
    pass
    

@app.get("/api/v1/marketplace/items")
async def list_marketplace_items():
    try:
        conn = get_db_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)

        cur.execute("SELECT id, name, price, description, address, contact_info FROM marketplace_items")
        items = cur.fetchall()

        cur.close()
        conn.close()
        return items
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
    
@app.get("/api/v1/marketplace/items/{id}")
async def get_marketplace_item(id: int):
    try:
        conn = get_db_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)

        cur.execute("SELECT id, name, price, description, address, contact_info FROM marketplace_items WHERE id = %s", (id,))
        item = cur.fetchone()

        cur.close()
        conn.close()
        if not item:
            raise HTTPException(status_code=404, detail="Item not found")
        return item
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    


@app.post("/api/v1/geo/location")
async def update_location(data: LocationUpdate):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        
        # SQL com PostGIS: ST_SetSRID e ST_MakePoint
        insert_query = """
        INSERT INTO devices (device_id, location, accuracy)
        VALUES (%s, ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s)
        """
        cur.execute(insert_query, (data.device_id, data.longitude, data.latitude, data.accuracy))
        
        conn.commit()
        cur.close()
        conn.close()
        return {"message": "Localização guardada!"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
@app.post("/api/v1/geo/map-data")
#Endpoint para descarregar dados geográficos para visualização de mapas
async def get_map_data():
    pass