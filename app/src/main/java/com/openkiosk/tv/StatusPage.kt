package com.openkiosk.tv

import android.content.Context

/** Páginas locales que se muestran cuando no hay red o falta configurar el kiosco. */
object StatusPage {

    fun offline(ctx: Context): String =
        page(ctx.getString(R.string.offline_title), ctx.getString(R.string.offline_body), pulse = true)

    fun setup(ctx: Context): String =
        page(ctx.getString(R.string.setup_title), ctx.getString(R.string.setup_body), pulse = false)

    private fun page(title: String, body: String, pulse: Boolean): String {
        val dot = if (pulse) "<div class=\"dot\"></div>" else ""
        return """<!doctype html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
html,body{height:100%;margin:0;background:#0e1a1c;color:#f3efe6;font-family:sans-serif;
display:flex;align-items:center;justify-content:center;text-align:center}
h1{font-weight:600;font-size:2.4rem;margin:0 0 .4em}
p{color:#a9c0b9;font-size:1.25rem;margin:0 auto;max-width:36em;line-height:1.5}
.dot{width:14px;height:14px;border-radius:50%;background:#5fc9c0;margin:0 auto 24px;
animation:p 1.4s ease-in-out infinite}
@keyframes p{0%,100%{opacity:.25}50%{opacity:1}}
</style></head><body><div>$dot<h1>${esc(title)}</h1><p>${esc(body)}</p></div></body></html>"""
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
