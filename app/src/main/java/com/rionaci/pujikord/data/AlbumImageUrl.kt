package com.rionaci.pujikord.data

fun albumImageUrl(url: String): String {
    if (!url.startsWith("https://www.psalmnote.com/")) return url
    return url.replace("/assets/img/albums/", "/album-image/")
}
