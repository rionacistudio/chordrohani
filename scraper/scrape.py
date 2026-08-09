"""Scrape chord dari jrchord.com → upsert ke Supabase (REST).
Hanya scrape lagu yang lastmod-nya lebih baru daripada di DB."""

import os
import re
import time

import requests
from bs4 import BeautifulSoup

# ── Config ──────────────────────────────────────────────────────────────
SUPABASE_URL = os.environ["SUPABASE_URL"]
SUPABASE_KEY = os.environ["SUPABASE_KEY"]
TABLE = "tb_chord"

SITEMAP_URL = "https://www.jrchord.com/sitemap_index.xml"
HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}

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
    print(f"  DEBUG URL: {SUPABASE_URL}/rest/v1/{TABLE}", flush=True)
    print(f"  DEBUG rows sample: {rows[0] if rows else 'empty'}", flush=True)
    r = requests.post(url, headers={
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=merge-duplicates",
    }, json=rows, timeout=60)
    print(f"  DEBUG status: {r.status_code}", flush=True)
    print(f"  DEBUG response: {r.text[:500]}", flush=True)
    r.raise_for_status()
    print(f"  ↑ Upserted {len(rows)} baris", flush=True)


# ── Sitemap (rekursif, pakai BeautifulSoup xml) ─────────────────────────

def get_all_urls(sitemap_url: str) -> list[dict]:
    """Ambil [{url, lastmod}] dari sitemap secara rekursif."""
    result = []
    try:
        r = requests.get(sitemap_url, headers=HEADERS, timeout=15)
        if r.status_code != 200:
            return result
        soup = BeautifulSoup(r.content, "xml")

        sub_sitemaps = soup.find_all("sitemap")
        if sub_sitemaps:
            for sub in sub_sitemaps:
                loc = sub.find("loc").text
                if "post-sitemap" in loc:
                    result.extend(get_all_urls(loc))
                    time.sleep(0.5)
        else:
            for url_tag in soup.find_all("url"):
                loc = url_tag.find("loc")
                lastmod = url_tag.find("lastmod")
                if loc:
                    result.append({
                        "url": loc.text,
                        "lastmod": lastmod.text if lastmod else "",
                    })
    except Exception as e:
        print(f"Error sitemap: {e}", flush=True)
    return result


# ── Scraping (sama seperti script asli) ─────────────────────────────────

def scrape_song(url: str) -> dict | None:
    try:
        r = requests.get(url, headers=HEADERS, timeout=15)
        if r.status_code != 200:
            return None

        soup = BeautifulSoup(r.text, "html.parser")
        all_pre = soup.find_all("pre")
        if not all_pre:
            return None

        # base_key dari <pre> pertama
        base_key = all_pre[0].get("data-key") or "C"

        cleaned = []
        for pre in all_pre:
            # Hapus <span> pertama (judul)
            first_span = pre.find("span")
            if first_span:
                first_span.decompose()

            raw = pre.get_text()
            # Hapus sisa tag span
            raw = re.sub(r"<span[^>]*>", "", raw)
            raw = raw.replace("</span>", "")

            # Rapikan: rstrip saja, jangan strip kiri (spasi penting untuk chord)
            lines = [line.rstrip() for line in raw.splitlines()]

            # Hapus baris pertama jika judul
            if lines and ("Chord " in lines[0] or "Lirik " in lines[0]):
                lines.pop(0)

            text = "\n".join(line for line in lines if line.strip())
            cleaned.append(text)

        chord_text = "\n\n".join(cleaned)

        # Judul
        judul = "Unknown Title"
        tag = soup.find(class_="song-detail-title") or soup.find(id="song-detail-title")
        if tag:
            judul = tag.text.strip()
        elif soup.find("h1"):
            judul = soup.find("h1").text.strip()

        # Penyanyi
        penyanyi = "Unknown Artist"
        tag = soup.find(class_="song-detail-artist") or soup.find(id="song-detail-artist")
        if tag:
            penyanyi = tag.text.strip()

        if not judul or not chord_text:
            return None

        return {
            "judul": judul,
            "penyanyi": penyanyi,
            "base_key": base_key,
            "isi_chord": chord_text,
        }
    except Exception as e:
        print(f"  ✗ Error: {e}", flush=True)
        return None


# ── Main ────────────────────────────────────────────────────────────────

def main():
    print("Ambil sitemap...", flush=True)
    all_entries = get_all_urls(SITEMAP_URL)
    print(f"   {len(all_entries)} URL ditemukan", flush=True)

    print("Ambil data existing dari Supabase...", flush=True)
    existing = supabase_get({
        "select": "judul,penyanyi,lastmod",
        "limit": "10000",
    })
    db_lastmod = {}
    for s in existing:
        db_lastmod[s["judul"]] = s.get("lastmod", "")
    print(f"   {len(db_lastmod)} lagu sudah ada", flush=True)

    # Filter: hanya yang lastmod lebih baru atau belum ada
    to_scrape = []
    for entry in all_entries:
        judul_slug = entry["url"].rstrip("/").split("/")[-1].replace("-", " ")
        stored = db_lastmod.get(judul_slug, "")
        if not stored or entry["lastmod"] > stored:
            to_scrape.append(entry)

    print(f"Perlu scrape: {len(to_scrape)} (skip {len(all_entries) - len(to_scrape)} up-to-date)", flush=True)

    if not to_scrape:
        print("Tidak ada update. Selesai.", flush=True)
        return

    upsert_batch = []
    success = 0
    skip = 0

    for i, entry in enumerate(to_scrape, 1):
        if i % 50 == 0:
            print(f"   ... {i}/{len(to_scrape)}", flush=True)
        data = scrape_song(entry["url"])
        if data:
            data["lastmod"] = entry["lastmod"]
            upsert_batch.append(data)
            success += 1
        else:
            skip += 1
        time.sleep(0.5)

    print(f"\nScraping selesai: {success} sukses, {skip} dilewati", flush=True)

    if not upsert_batch:
        print("Tidak ada data baru/update.", flush=True)
        return

    print(f"Upsert {len(upsert_batch)} lagu ke Supabase...", flush=True)
    for i in range(0, len(upsert_batch), 200):
        supabase_upsert(upsert_batch[i : i + 200])

    print("Selesai!", flush=True)


if __name__ == "__main__":
    main()
