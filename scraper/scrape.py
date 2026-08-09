"""Scrape chord dari jrchord.com → upsert ke Supabase (REST).
Hanya scrape lagu yang lastmod-nya lebih baru daripada di DB."""

import os
import re
import time
import xml.etree.ElementTree as ET

import requests
from bs4 import BeautifulSoup

# ── Config ──────────────────────────────────────────────────────────────
SUPABASE_URL = os.environ["https://gyyzutfqhkvkdtdlgtzo.supabase.co"]
SUPABASE_KEY = os.environ["eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd5eXp1dGZxaGt2a2R0ZGxndHpvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU2NzU5MzQsImV4cCI6MjEwMTI1MTkzNH0.Xzzm-mgyzyjY5dp-BsKtQ5mPBQEoNVIxKkZmYckwPBg"]
TABLE = "tb_chord"

SITEMAP_INDEX = "https://www.jrchord.com/sitemap.xml"
HEADERS = {"User-Agent": "ChordRhaniBot/1.0 (auto-sync)"}
NS = {"s": "http://www.sitemaps.org/schemas/sitemap/0.9"}

# ── Helpers ─────────────────────────────────────────────────────────────

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
        print(f"  ✗ Upsert gagal: {r.status_code} {r.text[:500]}")
    r.raise_for_status()
    print(f"  ↑ Upserted {len(rows)} baris")


# ── Sitemap ─────────────────────────────────────────────────────────────

def get_sitemap_urls() -> dict[str, str]:
    """Ambil {url: lastmod} dari semua post-sitemap."""
    r = requests.get(SITEMAP_INDEX, headers=HEADERS, timeout=20)
    r.raise_for_status()
    root = ET.fromstring(r.content)

    result = {}
    for sitemap in root.findall("s:sitemap", NS):
        loc = sitemap.find("s:loc", NS).text
        if "post-sitemap" not in loc:
            continue
        r2 = requests.get(loc, headers=HEADERS, timeout=20)
        r2.raise_for_status()
        sub = ET.fromstring(r2.content)
        for url_el in sub.findall("s:url", NS):
            u = url_el.find("s:loc", NS).text
            lm = url_el.find("s:lastmod", NS)
            result[u] = lm.text if lm is not None else ""
    return result


# ── Scraping ────────────────────────────────────────────────────────────

def scrape_song(url: str) -> dict | None:
    r = requests.get(url, headers=HEADERS, timeout=20)
    if r.status_code != 200:
        print(f"  ✗ {r.status_code} {url}")
        return None

    soup = BeautifulSoup(r.text, "lxml")
    title_el = soup.select_one("h1.song-detail-title")
    artist_el = soup.select_one("div.song-detail-artist")
    pre_el = soup.select_one("pre[data-key]")
    if not pre_el:
        return None

    judul = (title_el.get_text(strip=True) if title_el else "")
    penyanyi = (artist_el.get_text(strip=True) if artist_el else "")
    base_key = pre_el.get("data-key", "")
    chord_text = pre_el.get_text()

    lines = chord_text.split("\n")
    while lines and re.match(r"^(chord|kunci)\s+", lines[0], re.I):
        lines.pop(0)
    chord_text = "\n".join(lines).strip()

    if not judul or not chord_text:
        return None

    return {
        "judul": judul,
        "penyanyi": penyanyi,
        "base_key": base_key,
        "isi_chord": chord_text,
    }


# ── Main ────────────────────────────────────────────────────────────────

def main():
    print("Ambil sitemap...")
    sitemap = get_sitemap_urls()
    print(f"   {len(sitemap)} URL ditemukan")

    print("Ambil data existing dari Supabase...")
    existing = supabase_get({
        "select": "judul,penyanyi,lastmod",
        "limit": "10000",
    })
    db_lastmod = {}
    for s in existing:
        db_lastmod[s["judul"]] = s.get("lastmod", "")
    print(f"   {len(db_lastmod)} lagu sudah ada")

    # Filter: hanya URL yang lastmod-nya lebih baru atau belum ada di DB
    to_scrape = []
    for url, lm in sitemap.items():
        judul_slug = url.rstrip("/").split("/")[-1].replace("-", " ")
        stored_lm = db_lastmod.get(judul_slug, "")
        if not stored_lm or lm > stored_lm:
            to_scrape.append((url, lm))

    print(f"Perlu scrape: {len(to_scrape)} lagu (skip {len(sitemap) - len(to_scrape)} yang sudah up-to-date)")

    if not to_scrape:
        print("Tidak ada update. Selesai.")
        return

    upsert_batch = []
    for i, (url, lm) in enumerate(to_scrape, 1):
        if i % 20 == 0:
            print(f"   ... {i}/{len(to_scrape)}")
        data = scrape_song(url)
        if data:
            data["lastmod"] = lm
            upsert_batch.append(data)
        time.sleep(0.3)

    if not upsert_batch:
        print("Tidak ada data baru/update.")
        return

    print(f"\nUpsert {len(upsert_batch)} lagu ke Supabase...")
    for i in range(0, len(upsert_batch), 200):
        supabase_upsert(upsert_batch[i : i + 200])

    print("Selesai!")


if __name__ == "__main__":
    main()
