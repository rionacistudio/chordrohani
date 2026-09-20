"""
Migrate album images to Supabase Storage using iTunes Search API.
Psalmnote blocks direct image downloads, so we use iTunes artwork instead.
Run via GitHub Actions or locally.
"""

import hashlib
import io
import os
import re
import time
import traceback
from urllib.parse import urlparse

import requests
from PIL import Image
from supabase import create_client

SUPABASE_URL = os.environ.get("SUPABASE_URL", "https://gyyzutfqhkvkdtdlgtzo.supabase.co")
SERVICE_ROLE_KEY = os.environ.get("SERVICE_ROLE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd5eXp1dGZxaGt2a2R0ZGxndHpvIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4NTY3NTkzNCwiZXhwIjoyMTAxMjUxOTM0fQ.s_ascHVncvjHywADAri4oM0IFuOkjwMKGHyF-p1-ANc")
BUCKET = "album-images"
MAX_SIZE = (300, 300)
MAX_BYTES = 500 * 1024

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


def filename_from_album(album: str, artist: str) -> str:
    key = f"{artist}|{album}".lower()
    h = hashlib.md5(key.encode()).hexdigest()[:12]
    return f"{h}.jpg"


def search_itunes(album: str, artist: str) -> str | None:
    """Search iTunes for album artwork. Returns 600x600 URL or None."""
    query = f"{artist} {album}".strip()
    if not query:
        return None
    try:
        r = requests.get(
            "https://itunes.apple.com/search",
            params={"term": query, "entity": "album", "limit": 3},
            timeout=15,
        )
        r.raise_for_status()
        data = r.json()
        if data.get("resultCount", 0) == 0:
            return None

        # Cari match terbaik
        query_lower = query.lower()
        for result in data["results"]:
            result_name = result.get("collectionName", "").lower()
            result_artist = result.get("artistName", "").lower()
            if (album.lower() in result_name or result_name in album.lower()) and \
               (artist.lower() in result_artist or result_artist in artist.lower()):
                url = result.get("artworkUrl100", "")
                return url.replace("100x100", "600x600") if url else None

        # Fallback: pakai result pertama
        url = data["results"][0].get("artworkUrl100", "")
        return url.replace("100x100", "600x600") if url else None
    except Exception:
        return None


def main():
    sb = create_client(SUPABASE_URL, SERVICE_ROLE_KEY)

    print("Fetching songs with album info from DB...", flush=True)
    rows = sb.table("tb_chord").select("album,album_image").execute().data

    # Build unique album list: {(album_name, current_image_url)}
    albums = {}
    for r in rows:
        album = (r.get("album") or "").strip()
        img = (r.get("album_image") or "").strip()
        if album and album not in albums:
            albums[album] = img

    print(f"Found {len(albums)} unique albums", flush=True)

    # Check existing files in bucket
    try:
        existing = sb.storage.from_(BUCKET).list()
        existing_names = {f["name"] for f in existing}
    except Exception:
        existing_names = set()

    total = len(albums)
    uploaded = 0
    skipped = 0

    for i, (album, old_image) in enumerate(albums.items(), 1):
        fname = filename_from_album(album, "")
        new_url = f"{SUPABASE_URL}/storage/v1/object/public/{BUCKET}/{fname}"

        # Skip if already uploaded
        if fname in existing_names:
            print(f"[{i}/{total}] SKIP (exists): {album}", flush=True)
            skipped += 1
            try:
                sb.table("tb_chord").update({"album_image": new_url}).eq("album", album).neq("album_image", new_url).execute()
            except Exception:
                pass
            continue

        # Search iTunes
        art_url = search_itunes(album, "")
        if not art_url:
            print(f"[{i}/{total}] NO ART: {album}", flush=True)
            errors.append((album, "no iTunes match"))
            continue

        # Download
        try:
            resp = requests.get(art_url, timeout=30)
            resp.raise_for_status()
        except Exception as e:
            print(f"[{i}/{total}] FAIL download: {album} -> {e}", flush=True)
            errors.append((album, str(e)))
            continue

        # Compress
        try:
            compressed = compress_image(resp.content)
        except Exception as e:
            print(f"[{i}/{total}] FAIL compress: {album} -> {e}", flush=True)
            errors.append((album, str(e)))
            continue

        # Upload
        try:
            sb.storage.from_(BUCKET).upload(
                path=fname,
                file=compressed,
                file_options={"content-type": "image/jpeg", "upsert": "true"},
            )
        except Exception as e:
            print(f"[{i}/{total}] FAIL upload: {album} -> {e}", flush=True)
            errors.append((album, str(e)))
            continue

        # Update DB: set all rows with this album to new image
        try:
            sb.table("tb_chord").update({"album_image": new_url}).eq("album", album).execute()
        except Exception as e:
            print(f"[{i}/{total}] FAIL update DB: {album} -> {e}", flush=True)
            errors.append((album, str(e)))

        uploaded += 1
        size_kb = len(compressed) / 1024
        print(f"[{i}/{total}] OK: {album} ({size_kb:.0f}KB)", flush=True)
        time.sleep(0.5)

    print(f"\nDone! Uploaded: {uploaded}, Skipped: {skipped}, Errors: {len(errors)}", flush=True)
    if errors:
        with open("errors.log", "w") as f:
            for name, err in errors:
                f.write(f"{name}\t{err}\n")
        print("Errors saved to errors.log", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        traceback.print_exc()
