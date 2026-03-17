from typing import Optional
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import psycopg2
from psycopg2.extras import RealDictCursor
from psycopg2.extras import execute_values

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
    id: Optional[int] = None
    name: str
    description: str
    price: float
    address: str
    contact_info: str
    latitude: float
    longitude: float


@app.post("/api/v1/sync/locations")
async def sync_locations(locations: list[LocationUpdate]):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        # O NOME DA COLUNA É 'location'. A função vai no template.
        query = "INSERT INTO devices (device_id, location, accuracy) VALUES %s"
        template = "(%s, ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s)"
        values = [(loc.device_id, loc.longitude, loc.latitude, loc.accuracy) for loc in locations]
        
        execute_values(cur, query, values, template=template)
        conn.commit()
        return {"message": "Sincronizado!"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cur.close()
        conn.close()


    
@app.post("/api/v1/sync/items")
async def post_sync_items(items: list[MarketplaceItem]):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        query = "INSERT INTO marketplace_items (name, description, price, address, contact_info, location) VALUES %s"
        # O template mapeia cada campo e aplica a função geográfica ao longitude e latitude
        template = "(%s, %s, %s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326))"
        values = [(i.name, i.description, i.price, i.address, i.contact_info, i.longitude, i.latitude) for i in items]
        
        execute_values(cur, query, values, template=template)
        conn.commit()
        return {"status": "success", "uploaded": len(items)}
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cur.close()
        conn.close()



@app.get("/api/v1/sync")
async def get_sync_data(last_sync: str = "1970-01-01T00:00:00"):
    try:
        conn = get_db_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)
        # Seleciona apenas o que foi criado/alterado após a última sincronização da App
        cur.execute("""
            SELECT * FROM marketplace_items 
            WHERE created_at > %s
        """, (last_sync,))
        
        changes = cur.fetchall()
        return {"timestamp": "now()", "changes": changes}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cur.close()
        conn.close()


        
@app.post("/api/v1/items")
async def create_marketplace_item(item: MarketplaceItem):
    try:
        conn = get_db_connection()
        cur = conn.cursor()

        insert_query = """
        INSERT INTO marketplace_items (name, price, description, address, contact_info, location)
        VALUES (%s, %s, %s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326))
        """
        cur.execute(insert_query, (
            item.name,
            item.price,
            item.description,
            item.address,
            item.contact_info,
            item.longitude,
            item.latitude
        ))

        conn.commit()
        cur.close()
        conn.close()
        return {"message": "Item criado com sucesso!"}
    except Exception as e:
        print(f"ERRO DE SQL: {e}") # ISTO VAI APARECER NO TEU TERMINAL
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/items")
async def list_marketplace_items():
    try:
        conn = get_db_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)

        cur.execute("SELECT id, name, price, description, address, contact_info, ST_AsText(location) as location FROM marketplace_items")
        items = cur.fetchall()

        cur.close()
        conn.close()
        return items
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
    
@app.get("/api/v1/items/{id}")
async def get_marketplace_item(id: int):
    try:
        conn = get_db_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)

        cur.execute("SELECT id, name, price, description, address, contact_info, location FROM marketplace_items WHERE id = %s", (id,))
        item = cur.fetchone()

        cur.close()
        conn.close()
        if not item:
            raise HTTPException(status_code=404, detail="Item not found")
        return item
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    


@app.post("/api/v1/location")
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
    
@app.post("/api/v1/map-data")
#Endpoint para descarregar dados geográficos para visualização de mapas
async def get_map_data():
    pass