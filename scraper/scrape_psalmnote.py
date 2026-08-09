"""Scrape chord dari psalmnote.com API → upsert ke Supabase (REST)."""

import os
import time

import requests

# ── Config ──────────────────────────────────────────────────────────────
SUPABASE_URL = os.environ["SUPABASE_URL"]
SUPABASE_KEY = os.environ["SUPABASE_KEY"]
TABLE = "tb_chord"

API_BASE = "https://www.psalmnote.com/api"
HEADERS = {"User-Agent": "ChordRhaniBot/1.0 (auto-sync)"}

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
    """Konversi song array psalmnote ke format teks chord-over-lyric."""
    parts = song_data.get("song", [])
    if not parts:
        return ""

    # Index by part name for "equals" lookup
    part_map = {}
    for p in parts:
        if "part" in p and "content" in p:
            part_map[p["part"]] = p["content"]

    lines = []
    for section in parts:
        # Handle "equals" (repeat section)
        if "equals" in section:
            ref = section["equals"]
            content = part_map.get(ref, [])
            part_name = ref
        else:
            part_name = section.get("part", "")
            content = section.get("content", [])

        if not part_name:
            continue

        # Section header
        lines.append(f"{part_name}:")

        for row in content:
            if row.get("row") == "chords":
                lines.append(row.get("chordsLine", "").rstrip())
            elif row.get("row") == "lyric":
                lines.append(row.get("lyric", "").rstrip())

        lines.append("")  # blank line between sections

    return "\n".join(lines).strip()


# ── Main ────────────────────────────────────────────────────────────────

def main():
    print("Ambil daftar lagu dari psalmnote...", flush=True)
    r = requests.get(f"{API_BASE}/songs", headers=HEADERS, timeout=30)
    r.raise_for_status()
    all_songs = r.json()
    print(f"   {len(all_songs)} lagu ditemukan", flush=True)

    # Filter hanya lagu Indonesia + verified
    indo_songs = [s for s in all_songs if s.get("languageCode") == "ind" and s.get("isVerified") == 1]
    print(f"   {len(indo_songs)} lagu Indonesia (verified)", flush=True)

    print("Ambil data existing dari Supabase...", flush=True)
    existing = supabase_get({
        "select": "judul,penyanyi,lastmod",
        "limit": "10000",
    })
    db_keys = {(s["judul"], s["penyanyi"]) for s in existing}
    print(f"   {len(db_keys)} lagu sudah ada di DB", flush=True)

    # Scrape semua (update juga yang sudah ada)
    to_scrape = indo_songs
    print(f"Scraping {len(to_scrape)} lagu dari psalmnote...", flush=True)

    upsert_batch = []
    success = 0
    fail = 0

    for i, song in enumerate(to_scrape, 1):
        if i % 50 == 0:
            print(f"   ... {i}/{len(to_scrape)}", flush=True)

        alias = song.get("alias", "")
        if not alias:
            fail += 1
            continue

        try:
            r = requests.get(f"{API_BASE}/song/{alias}", headers=HEADERS, timeout=15)
            if r.status_code != 200:
                fail += 1
                continue

            detail = r.json()
            chord_text = song_to_text(detail)

            if not chord_text:
                fail += 1
                continue

            judul = detail.get("title", "").strip()
            penyanyi = detail.get("artist", "").strip()
            base_key = detail.get("chordBase", "").strip()

            # Info tambahan dari songinfoObj
            info = detail.get("songinfoObj") or {}
            album_obj = info.get("albumObj") or {}
            album = album_obj.get("name", "")
            album_image = album_obj.get("imageUrl", "")
            if album_image:
                album_image = f"https://www.psalmnote.com/assets/img/albums/{album_image}"
            songwriter = info.get("songwriter", "") or ""
            year = str(album_obj.get("publishedYear", "") or "")
            songtype = (detail.get("songtypeObj") or {}).get("songtype", "") or ""

            if not judul:
                fail += 1
                continue

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
            })
            success += 1

        except Exception as e:
            print(f"  Error {alias}: {e}", flush=True)
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
