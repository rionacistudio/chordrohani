"""
Migrate album images from psalmnote.com to Supabase Storage.
Run locally or via GitHub Actions (psalmnote blocks local IP).
"""

import hashlib
import io
import os
import socket
import time
import traceback
from urllib.parse import urlparse

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from PIL import Image
from supabase import create_client

SUPABASE_URL = os.environ.get("SUPABASE_URL", "https://gyyzutfqhkvkdtdlgtzo.supabase.co")
SERVICE_ROLE_KEY = os.environ.get("SERVICE_ROLE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd5eXp1dGZxaGt2a2R0ZGxndHpvIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4NTY3NTkzNCwiZXhwIjoyMTAxMjUxOTM0fQ.s_ascHVncvjHywADAri4oM0IFuOkjwMKGHyF-p1-ANc")
BUCKET = "album-images"
MAX_SIZE = (300, 300)
MAX_BYTES = 500 * 1024
HEADERS = {"User-Agent": "ChordRhaniBot/1.0 (auto-sync)"}

# IPv4 forcing (sama seperti scraper)
_original_getaddrinfo = socket.getaddrinfo

def ipv4_getaddrinfo(host, port, family=0, type=0, proto=0, flags=0):
    return _original_getaddrinfo(host, port, socket.AF_INET, type, proto, flags)

socket.getaddrinfo = ipv4_getaddrinfo

# Session dengan retry (sama seperti scraper)
session = requests.Session()
retry = Retry(
    total=12,
    connect=12,
    read=12,
    backoff_factor=3,
    status_forcelist=[429, 500, 502, 503, 504],
    allowed_methods=["GET"],
)
session.mount("https://", HTTPAdapter(max_retries=retry))

errors = []


def compress_image(data: bytes) -> bytes:
    img = Image.open(io.BytesIO(data))
    if img.mode in ("RGBA", "P"):
        img = img.convert("RGB")
    img.thumbnail(MAX_SIZE, Image.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=80, optimize=True)
    out = buf.getvalue()
    if len(out) > MAX_BYTES:
        q = max(10, int(80 * MAX_BYTES / len(out)))
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=q, optimize=True)
        out = buf.getvalue()
    return out


def filename_from_url(url: str) -> str:
    path = urlparse(url).path
    name = path.rsplit("/", 1)[-1]
    if not name or "." not in name:
        ext = hashlib.md5(url.encode()).hexdigest()[:8]
        name = f"{ext}.jpg"
    base, ext = name.rsplit(".", 1)
    return f"{base}.{ext.lower()}"


def main():
    sb = create_client(SUPABASE_URL, SERVICE_ROLE_KEY)

    print("Fetching album_image URLs from DB...", flush=True)
    rows = sb.table("tb_chord").select("album_image").execute().data
    urls = sorted({r["album_image"] for r in rows if r.get("album_image") and r["album_image"].strip()})
    print(f"Found {len(urls)} unique album_image URLs", flush=True)

    try:
        existing = sb.storage.from_(BUCKET).list()
        existing_names = {f["name"] for f in existing}
    except Exception:
        existing_names = set()

    total = len(urls)
    uploaded = 0
    skipped = 0

    for i, url in enumerate(urls, 1):
        fname = filename_from_url(url)
        new_url = f"{SUPABASE_URL}/storage/v1/object/public/{BUCKET}/{fname}"

        if fname in existing_names:
            print(f"[{i}/{total}] SKIP (exists): {fname}", flush=True)
            skipped += 1
            try:
                sb.table("tb_chord").update({"album_image": new_url}).eq("album_image", url).execute()
            except Exception:
                pass
            continue

        # Download via requests (sama seperti scraper API)
        try:
            resp = session.get(url, headers=HEADERS, timeout=30)
            resp.raise_for_status()
        except Exception as e:
            print(f"[{i}/{total}] FAIL download: {fname} -> {e}", flush=True)
            errors.append((url, str(e)))
            continue

        # Compress
        try:
            compressed = compress_image(resp.content)
        except Exception as e:
            print(f"[{i}/{total}] FAIL compress: {fname} -> {e}", flush=True)
            errors.append((url, str(e)))
            continue

        # Upload
        try:
            sb.storage.from_(BUCKET).upload(
                path=fname,
                file=compressed,
                file_options={"content-type": "image/jpeg", "upsert": "true"},
            )
        except Exception as e:
            print(f"[{i}/{total}] FAIL upload: {fname} -> {e}", flush=True)
            errors.append((url, str(e)))
            continue

        # Update DB
        try:
            sb.table("tb_chord").update({"album_image": new_url}).eq("album_image", url).execute()
        except Exception as e:
            print(f"[{i}/{total}] FAIL update DB: {fname} -> {e}", flush=True)
            errors.append((url, str(e)))

        uploaded += 1
        size_kb = len(compressed) / 1024
        print(f"[{i}/{total}] OK: {fname} ({size_kb:.0f}KB)", flush=True)
        time.sleep(0.3)

    print(f"\nDone! Uploaded: {uploaded}, Skipped: {skipped}, Errors: {len(errors)}", flush=True)
    if errors:
        with open("errors.log", "w") as f:
            for url, err in errors:
                f.write(f"{url}\t{err}\n")
        print("Errors saved to errors.log", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        traceback.print_exc()
