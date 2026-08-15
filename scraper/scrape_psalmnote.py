"""Scrape chord dari psalmnote.com API → upsert ke Supabase (REST).
Strategi: artists → albums → songinfos → song detail."""

import os
import time

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

SUPABASE_URL = os.environ["SUPABASE_URL"]
SUPABASE_KEY = os.environ["SUPABASE_KEY"]
TABLE = "tb_chord"

API_BASE = "https://www.psalmnote.com/api"
HEADERS = {"User-Agent": "ChordRhaniBot/1.0 (auto-sync)"}

# Session dengan retry
session = requests.Session()
retry = Retry(total=5, backoff_factor=2, status_forcelist=[500, 502, 503, 504])
session.mount("https://", HTTPAdapter(max_retries=retry))

# ── Supabase ────────────────────────────────────────────────────────────

def supabase_get(params: dict) -> list:
    url = f"{SUPABASE_URL}/rest/v1/{TABLE}"
    r = requests.get(url, headers={
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Accept": "application/json",
    }, params=params, timeout=30)
    r.raise_for_status()
    return r.json()


def supabase_upsert(rows: list):
    url = f"{SUPABASE_URL}/rest/v1/{TABLE}?on_conflict=judul,penyanyi"
    r = requests.post(url, headers={
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=merge-duplicates",
    }, json=rows, timeout=60)
    if not r.ok:
        print(f"  Upsert gagal: {r.status_code} {r.text[:500]}", flush=True)
    r.raise_for_status()
    print(f"  Upserted {len(rows)} baris", flush=True)


# ── Konversi JSON psalmnote → teks chord ────────────────────────────────

def song_to_text(song_data: dict) -> str:
    parts = song_data.get("song", [])
    if not parts:
        return ""

    part_map = {}
    for p in parts:
        if "part" in p and "content" in p:
            part_map[p["part"]] = p["content"]

    lines = []
    for section in parts:
        if "equals" in section:
            ref = section["equals"]
            content = part_map.get(ref, [])
            part_name = ref
        else:
            part_name = section.get("part", "")
            content = section.get("content", [])

        if not part_name:
            continue

        lines.append(f"{part_name}:")

        for row in content:
            if row.get("row") == "chords":
                lines.append(row.get("chordsLine", "").rstrip())
            elif row.get("row") == "lyric":
                lines.append(row.get("lyric", "").rstrip())

        lines.append("")

    return "\n".join(lines).strip()


# ── Main ────────────────────────────────────────────────────────────────

def main():
    # Step 1: Ambil semua artis + album
    print("Step 1: Ambil daftar artis...", flush=True)
    r = session.get(f"{API_BASE}/artists", headers=HEADERS, timeout=60)
    r.raise_for_status()
    artists = r.json()
    print(f"   {len(artists)} artis ditemukan", flush=True)

    # Kumpulkan semua album alias
    all_albums = []
    for artist in artists:
        for album in artist.get("albums", []):
            all_albums.append(album.get("alias", ""))
    all_albums = [a for a in all_albums if a]
    print(f"   {len(all_albums)} album ditemukan", flush=True)

    # Step 2: Ambil songinfos per album
    print("Step 2: Ambil daftar lagu per album...", flush=True)
    all_aliases = set()
    for i, album_alias in enumerate(all_albums, 1):
        if i % 100 == 0:
            print(f"   ... {i}/{len(all_albums)} album | {len(all_aliases)} lagu", flush=True)
        try:
            r = session.get(f"{API_BASE}/album/{album_alias}", headers=HEADERS, timeout=20)
            if r.status_code == 200:
                album_data = r.json()
                for info in album_data.get("songinfos", []):
                    song_obj = info.get("songObj")
                    if song_obj and song_obj.get("alias"):
                        all_aliases.add(song_obj["alias"])
        except Exception as e:
            pass
        time.sleep(0.2)

    print(f"   Total {len(all_aliases)} lagu unik ditemukan", flush=True)

    # Step 3: Scrape detail per lagu
    print(f"Step 3: Scrape detail {len(all_aliases)} lagu...", flush=True)

    # Ambil data existing untuk lastmod check
    existing = supabase_get({
        "select": "judul,penyanyi,lastmod",
        "limit": "10000",
    })
    db_keys = {(s["judul"], s["penyanyi"]) for s in existing}
    print(f"   {len(db_keys)} lagu sudah ada di DB", flush=True)

    upsert_batch = []
    success = 0
    fail = 0

    for i, alias in enumerate(all_aliases, 1):
        if i % 50 == 0:
            print(f"   ... {i}/{len(all_aliases)} | {success} ok, {fail} fail", flush=True)

        try:
            r = session.get(f"{API_BASE}/song/{alias}", headers=HEADERS, timeout=20)
            if r.status_code != 200:
                fail += 1
                continue

            detail = r.json()
            if not detail:
                fail += 1
                continue

            chord_text = song_to_text(detail)
            if not chord_text:
                fail += 1
                continue

            judul = detail.get("title", "").strip()
            penyanyi = detail.get("artist", "").strip()
            base_key = detail.get("chordBase", "").strip()

            if not judul:
                fail += 1
                continue

            info = detail.get("songinfoObj") or {}
            album_obj = info.get("albumObj") or {}
            album = album_obj.get("name", "")
            album_image = album_obj.get("imageUrl", "")
            if album_image:
                album_image = f"https://www.psalmnote.com/assets/img/albums/{album_image}"
            songwriter = info.get("songwriter", "") or ""
            year = str(album_obj.get("publishedYear", "") or "")
            songtype = (detail.get("songtypeObj") or {}).get("songtype", "") or ""
            language = detail.get("languageCode", "") or ""
            youtube_items = detail.get("youtube") or []
            youtube_url = ""
            if youtube_items:
                youtube = youtube_items[0] or {}
                youtube_url = youtube.get("fullUrl", "") or ""
                if not youtube_url and youtube.get("url"):
                    youtube_url = f"https://www.youtube.com/watch?v={youtube['url']}"

            upsert_batch.append({
                "judul": judul,
                "penyanyi": penyanyi,
                "base_key": base_key,
                "isi_chord": chord_text,
                "lastmod": detail.get("updatedAt", ""),
                "album": album,
                "album_image": album_image,
                "songwriter": songwriter,
                "year": year,
                "songtype": songtype,
                "language": language,
                "youtube_url": youtube_url,
            })
            success += 1

        except Exception as e:
            fail += 1

        time.sleep(0.3)

    print(f"\nScraping selesai: {success} sukses, {fail} gagal", flush=True)

    if not upsert_batch:
        print("Tidak ada data baru/update.", flush=True)
        return

    print(f"Upsert {len(upsert_batch)} lagu ke Supabase...", flush=True)
    for i in range(0, len(upsert_batch), 200):
        supabase_upsert(upsert_batch[i : i + 200])

    print("Selesai!", flush=True)


if __name__ == "__main__":
    main()
