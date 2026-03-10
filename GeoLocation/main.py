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



@app.post("/api/v1/geo/location")
async def update_location(data: LocationUpdate):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        
        # SQL com PostGIS: ST_SetSRID e ST_MakePoint
        insert_query = """
        INSERT INTO geo_resources (device_id, location, accuracy)
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
async def get_map_data():
    pass