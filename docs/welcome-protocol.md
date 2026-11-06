# OpenKiosk Welcome Protocol · v1

A small, open JSON format that tells an OpenKiosk TV **what to show** to guests. The kiosk draws it with its own native design, so any host can power a welcome screen without building a web page: a static file, a spreadsheet export, a serverless function, or a property-management tool can all produce it.

> Español: un formato JSON abierto que le dice a la TV qué mostrar al huésped. Ver el [resumen en español](#resumen-en-español) al final.

- **Typical provider:** a small serverless function (for example on Cloudflare Pages) that reads the Airbnb calendar on the server and serves this JSON, so the calendar link never reaches the TV.
- **Example document:** [`examples/welcome.json`](examples/welcome.json)

## Transport

| Rule | Value |
|---|---|
| Method | `GET` on the URL the host configures in the kiosk |
| Scheme | `https://` only |
| Response type | `application/json; charset=utf-8` |
| Maximum size | 256 KB (larger documents are rejected) |
| Refresh | The kiosk re-reads it every `ttl_minutes` (default 30, minimum 5) and keeps the last valid copy for offline use |
| Authentication | None required. If the provider needs to protect the URL, use an unguessable path or a token in the query string. Do not put secrets in the document: the TV shows it on screen. |

## Document

```jsonc
{
  "protocol": "openkiosk-welcome",   // required, fixed value
  "version": 1,                      // required
  "generated_at": "2026-11-06T06:00:00Z",
  "ttl_minutes": 30,

  "property": {
    "id": "beach-4b",
    "name": "Beach House 4B",                    // required
    "area": "Playa del Carmen",
    "timezone": "America/Cancun",                // IANA; defaults to the TV's time zone
    "accent": "#C9A46A",                         // optional brand color
    "logo_url": null,                            // optional https image
    "background_url": "https://example.com/bg.jpg"
  },

  "stay": {
    "phase": "in_stay",            // arrival | in_stay | checkout | vacant
    "guest_name": null,            // optional; omit if the provider does not know it
    "checkin": "2026-11-02",       // local dates, YYYY-MM-DD
    "checkout": "2026-11-10",
    "checkin_time": "15:00",
    "checkout_time": "11:00"
  },

  "wifi": { "ssid": "BeachHouse4B", "password": "…", "security": "WPA" },
  "host": { "name": "Your host", "whatsapp": "+52 000 000 0000", "hours": "9:00–21:00" },
  "quiet_hours": "22:00–07:00",

  "messages": {
    "welcome":  { "es": "¡Bienvenidos! …", "en": "Welcome! …" },
    "checkout": { "es": "Hoy es su salida…", "en": "Today is your check-out…" },
    "vacant":   { "es": "Preparando la casa para su llegada.", "en": "Getting the home ready for your arrival." }
  },

  "sections": [
    {
      "id": "essentials",
      "title": { "es": "Lo esencial", "en": "Good to know" },
      "items": [
        { "es": "Alberca 9:00–22:00", "en": "Pool 9:00–22:00" }
      ]
    }
  ]
}
```

### Localized text

Every human-readable field accepts either a plain string or an object of language codes:

```json
"title": "Lo esencial"
"title": { "es": "Lo esencial", "en": "Good to know" }
```

The kiosk picks the guest's chosen language, then the TV language, then `en`, then the first available.

### Stay phases

| Phase | When | What the kiosk shows |
|---|---|---|
| `arrival` | Check-in day | Welcome message, check-in time, WiFi first |
| `in_stay` | Between check-in and check-out day | Normal welcome screen |
| `checkout` | Check-out day | Check-out time and `messages.checkout`, plus a reminder to sign out of streaming apps |
| `vacant` | No current reservation | Neutral screen: property name and `messages.vacant`, **never a guest name** |

If `phase` is missing, the kiosk derives it from `checkin` / `checkout` in the property time zone. With no dates at all it uses `vacant`.

## What the kiosk does with it (security model)

- **Text is only text.** No HTML or Markdown is interpreted, and nothing in the document is executed.
- **Images** load only over `https`, from the provider's host or the hosts the host allows in settings.
- **Unknown fields are ignored**, so providers can add extensions without breaking older kiosks.
- **Invalid documents are rejected** and the last valid copy stays on screen.
- **Reserved:** an `admin` object for signed host commands (for example a remote PIN reset). Planned for v0.3 and ignored by v1 kiosks.

## Privacy guidance for providers

- Leave `guest_name` out unless the guest is happy to see it on a shared screen.
- Never include door codes, lockbox codes or alarm codes. The URL can be opened by anyone who has it.
- Keep calendar links (iCal) and API keys on the server, never in the document.

## Versioning

`version` changes only for breaking changes. New optional fields can appear at any time within v1.

---

## Resumen en español

- La TV hace un `GET` por `https` a la URL que configura el anfitrión y recibe este JSON (máximo 256 KB).
- Obligatorios: `protocol`, `version` y `property.name`. Todo lo demás es opcional.
- Los textos pueden ser una cadena o un objeto por idioma (`{ "es": "…", "en": "…" }`).
- `stay.phase` define qué se muestra: `arrival` (día de llegada), `in_stay`, `checkout` (día de salida) o `vacant` (sin huésped, **nunca muestra el nombre del anterior**).
- La TV no ejecuta nada del documento, solo muestra texto e imágenes `https`, y guarda la última copia válida para cuando no hay internet.
- No pongas códigos de puerta ni de caja de llaves, ni enlaces de calendario o llaves de API: la URL la puede abrir cualquiera que la tenga.
