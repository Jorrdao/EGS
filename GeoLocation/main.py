from typing import Optional
from datetime import datetime
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import psycopg2
import requests
import os
from psycopg2.extras import RealDictCursor
from psycopg2.extras import execute_values

app = FastAPI()

# ── InfluxDB config ────────────────────────────────────────────────────────────
INFLUX_URL    = "http://influxdb-service:8086"
INFLUX_TOKEN  = "stormos-super-secret-token"
INFLUX_ORG    = "stormos"
INFLUX_BUCKET = "stormos"

def write_metric(measurement: str, tags: dict, fields: dict):
    tag_str   = ",".join(f"{k}={v}" for k, v in tags.items())
    field_str = ",".join(f"{k}={v}" for k, v in fields.items())
    ts_ns     = int(datetime.utcnow().timestamp() * 1_000_000_000)
    line      = f"{measurement},{tag_str} {field_str} {ts_ns}"
    try:
        requests.post(
            f"{INFLUX_URL}/api/v2/write?org={INFLUX_ORG}&bucket={INFLUX_BUCKET}&precision=ns",
            headers={
                "Authorization": f"Token {INFLUX_TOKEN}",
                "Content-Type": "text/plain"
            },
            data=line,
            timeout=3
        )
    except Exception as e:
        print(f"InfluxDB write failed: {e}")

# ── DB connection ──────────────────────────────────────────────────────────────
def get_db_connection():
    return psycopg2.connect(
        host=os.getenv("POSTGRES_HOST", "localhost"),
        database=os.getenv("POSTGRES_DB", "geodb"),
        user=os.getenv("POSTGRES_USER", "user_geo"),
        password=os.getenv("POSTGRES_PASSWORD", "password_geo")
    )

# ── Models ─────────────────────────────────────────────────────────────────────
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


# ── Endpoints ──────────────────────────────────────────────────────────────────

@app.post("/api/v1/sync/locations")
async def sync_locations(locations: list[LocationUpdate]):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        query = "INSERT INTO devices (device_id, location, accuracy) VALUES %s"
        template = "(%s, ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s)"
        values = [(loc.device_id, loc.longitude, loc.latitude, loc.accuracy) for loc in locations]
        execute_values(cur, query, values, template=template)
        conn.commit()

        write_metric(
            measurement="device_locations",
            tags={"action": "sync"},
            fields={"count": f"{len(locations)}i"}
        )

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
        template = "(%s, %s, %s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326))"
        values = [(i.name, i.description, i.price, i.address, i.contact_info, i.longitude, i.latitude) for i in items]
        execute_values(cur, query, values, template=template)
        conn.commit()

        write_metric(
            measurement="marketplace_items",
            tags={"action": "sync"},
            fields={"count": f"{len(items)}i"}
        )

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

        write_metric(
            measurement="marketplace_items",
            tags={"action": "created"},
            fields={"count": "1i", "price": item.price}
        )

        return {"message": "Item criado com sucesso!"}
    except Exception as e:
        print(f"ERRO DE SQL: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cur.close()
        conn.close()


@app.get("/api/v1/items")
async def list_marketplace_items(radius: float = 0.0):
    try:
        conn = get_db_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)

        user_lat, user_lon = 40.6405, -8.6538

        if radius == 0:
            query = "SELECT id, name, price, description, address, contact_info, ST_AsText(location) as location FROM marketplace_items"
            cur.execute(query)
        else:
            query = """
            SELECT id, name, price, description, address, contact_info, ST_AsText(location) as location 
            FROM marketplace_items 
            WHERE ST_DWithin(location::geography, ST_SetSRID(ST_MakePoint(%s, %s), 4326)::geography, %s)
            """
            cur.execute(query, (user_lon, user_lat, radius * 1000))

        items = cur.fetchall()

        write_metric(
            measurement="marketplace_items",
            tags={"action": "listed"},
            fields={"count": f"{len(items)}i", "radius": radius}
        )

        return items
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cur.close()
        conn.close()


@app.get("/api/v1/items/{id}")
async def get_marketplace_item(id: int):
    try:
        conn = get_db_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute("SELECT id, name, price, description, address, contact_info, location FROM marketplace_items WHERE id = %s", (id,))
        item = cur.fetchone()

        if not item:
            raise HTTPException(status_code=404, detail="Item not found")

        write_metric(
            measurement="marketplace_items",
            tags={"action": "viewed"},
            fields={"count": "1i", "item_id": f"{id}i"}
        )

        return item
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cur.close()
        conn.close()


@app.post("/api/v1/location")
async def update_location(data: LocationUpdate):
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        insert_query = """
        INSERT INTO devices (device_id, location, accuracy)
        VALUES (%s, ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s)
        """
        cur.execute(insert_query, (data.device_id, data.longitude, data.latitude, data.accuracy))
        conn.commit()

        write_metric(
            measurement="device_locations",
            tags={"action": "update"},
            fields={"count": "1i", "accuracy": data.accuracy}
        )

        return {"message": "Localização guardada!"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cur.close()
        conn.close()


@app.get("/api/v1/map/data")
async def get_map_data(min_lat: float, max_lat: float, min_lon: float, max_lon: float):
    try:
        conn = get_db_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)
        query = """
            SELECT id, name, price, ST_X(location::geometry) as lon, ST_Y(location::geometry) as lat 
            FROM marketplace_items 
            WHERE location && ST_MakeEnvelope(%s, %s, %s, %s, 4326)
        """
        cur.execute(query, (min_lon, min_lat, max_lon, max_lat))
        items = cur.fetchall()

        write_metric(
            measurement="map_requests",
            tags={"action": "fetch"},
            fields={"count": f"{len(items)}i"}
        )

        return {"map_elements": items}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cur.close()
        conn.close()