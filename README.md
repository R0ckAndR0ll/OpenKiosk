# OpenKiosk

**Free, open-source kiosk launcher for Android TV.** Turn the TV in your Airbnb or vacation rental into a welcome screen for your guests: WiFi, house rules, check-in/check-out, contact info, whatever you want to show.

**Kiosco gratuito y de código abierto para Android TV.** Convierte la TV de tu Airbnb o renta vacacional en una pantalla de bienvenida para tus huéspedes. 👉 [Leer en español](#español)

- No accounts, no cloud, **no telemetry**. Everything stays on the TV.
- Works with **any web page** you host (your own site, a Google Site, a Notion page, a Cloudflare Pages site…).
- Licensed under **GPL-3.0**: free forever, for everyone.

---

## English

### What it does

| Feature | Details |
|---|---|
| Full-screen web view | Shows your welcome page, no browser bars. |
| Boots straight into it | Can be set as the TV's **home screen**: when the TV turns on, guests see your welcome screen. |
| Guest-proof | BACK and HOME don't take guests out. Settings are protected by a **host PIN**. |
| Domain allowlist | Only your domain (and the extra ones you allow) can load. Links to anywhere else are blocked. |
| Offline fallback | If WiFi drops, it shows the last cached copy, or a calm "Connecting…" screen, and retries every 30 s. |
| Self-healing | Reloads periodically and recreates the web engine if it crashes. |
| Screen always on | Keeps the display awake while the app is in front. |
| Remote setup via ADB | Configure URL and PIN from a computer, no typing with the remote. |
| Bilingual UI | English and Spanish (follows the TV language). |

### Install

1. Download the latest `OpenKiosk-x.y.z.apk` from **[Releases](../../releases)**.
2. Install it on the TV: with a USB stick and a file manager, the *Send Files to TV* app, or from a computer with ADB:
   ```bash
   adb connect <TV-IP>:5555
   adb install OpenKiosk-x.y.z.apk
   ```

### Configure

Open OpenKiosk. The first time it shows the settings screen:
- **Welcome page URL** (must be `https://`)
- **Host PIN** (4–8 digits; strongly recommended)
- **Reload interval** (minutes, `0` = never)
- **Extra allowed domains** (optional, e.g. a CDN your page needs for navigation)

Or set it from a computer, without the remote:
```bash
adb shell am start -n com.openkiosk.tv/.MainActivity \
  --es url "https://your-site.com/?house=1" \
  --es set_pin 1234 \
  --es reload_minutes 60
```
Once a PIN is set, changing the URL by ADB requires `--es pin <current PIN>`.

**Open settings later:** press **BACK 5 times**, or hold **OK** for 3 seconds, or press **MENU**.

### Start automatically when the TV turns on

The most reliable way is to make OpenKiosk the **home screen (launcher)**:
- Settings → **Set as home screen**, if your TV offers it, or
- From a computer:
  ```bash
  adb shell cmd package set-home-activity com.openkiosk.tv/.MainActivity
  ```

Some brands (e.g. TCL Google TV) also need autostart permission:
```bash
adb shell appops set com.openkiosk.tv APP_AUTO_START allow
adb shell appops set com.openkiosk.tv SYSTEM_ALERT_WINDOW allow
```

To undo: uninstall OpenKiosk or choose another home app in the TV settings.

### Don't have a welcome page yet?

Any web page works. A good one shows: guest name, check-in/out, WiFi with a QR code, house rules, and how to reach you. Keep **door codes and lockbox codes off the screen**: the page is reachable by URL, so treat it as public information.

### Security notes

- Only `https://` pages are allowed; plain `http` is blocked.
- Navigation is restricted to the configured domain and its subdomains.
- File and content access from the web view is disabled, and pop-up windows are blocked.
- Set a PIN. Without one, anyone with ADB access to the TV could change the URL.

### Build from source

The APK is built automatically by GitHub Actions on every push ([workflow](.github/workflows/build.yml)). To build locally you need JDK 17 and the Android SDK:
```bash
gradle assembleRelease
```
Releases are signed with the project key stored in the repository secrets. Forks and local builds are signed with the debug key.

### Roadmap

- [ ] Screen schedule (dim or turn off at night)
- [ ] Burn-in protection for 24/7 screens
- [ ] Optional remote config (one JSON for all your TVs)
- [ ] Android "lock task" mode for managed devices
- [ ] More languages

Ideas and pull requests are welcome.

### License

[GPL-3.0](LICENSE). You can use, study, share and modify OpenKiosk for free. If you distribute a modified version, it must stay open source under the same license.

---

## Español

### Qué hace

| Función | Detalle |
|---|---|
| Vista web a pantalla completa | Muestra tu página de bienvenida, sin barras de navegador. |
| Arranca directo | Se puede poner como **pantalla de inicio** de la TV: al encenderla, el huésped ve tu bienvenida. |
| A prueba de huéspedes | ATRÁS e INICIO no los sacan. Los ajustes se protegen con **PIN del anfitrión**. |
| Lista blanca de dominios | Solo carga tu dominio (y los extra que autorices). Cualquier otro enlace se bloquea. |
| Modo sin internet | Si se cae el WiFi, muestra la última copia guardada o una pantalla tranquila de "Conectando…", y reintenta cada 30 s. |
| Se recupera solo | Recarga periódica y reinicia el motor web si falla. |
| Pantalla siempre encendida | Mantiene la pantalla activa mientras la app está al frente. |
| Configuración remota por ADB | URL y PIN desde una computadora, sin teclear con el control. |
| Interfaz bilingüe | Español e inglés (según el idioma de la TV). |

### Instalar

1. Descarga el `OpenKiosk-x.y.z.apk` más reciente desde **[Releases](../../releases)**.
2. Instálalo en la TV: con una USB y un explorador de archivos, con la app *Send Files to TV*, o desde una computadora con ADB:
   ```bash
   adb connect <IP-de-la-TV>:5555
   adb install OpenKiosk-x.y.z.apk
   ```

### Configurar

Abre OpenKiosk. La primera vez muestra los ajustes:
- **URL de la pantalla de bienvenida** (debe ser `https://`)
- **PIN del anfitrión** (4 a 8 dígitos; muy recomendado)
- **Recarga** (minutos, `0` = nunca)
- **Dominios extra permitidos** (opcional)

O desde una computadora, sin el control:
```bash
adb shell am start -n com.openkiosk.tv/.MainActivity \
  --es url "https://tu-sitio.com/?casa=1" \
  --es set_pin 1234 \
  --es reload_minutes 60
```
Con PIN ya configurado, para cambiar la URL por ADB hay que agregar `--es pin <PIN actual>`.

**Abrir ajustes después:** presiona **ATRÁS 5 veces**, o mantén **OK** 3 segundos, o presiona **MENÚ**.

### Que arranque solo al encender la TV

Lo más confiable es poner OpenKiosk como **pantalla de inicio (launcher)**:
- Ajustes → **Usar como pantalla de inicio**, si tu TV lo ofrece, o
- Desde una computadora:
  ```bash
  adb shell cmd package set-home-activity com.openkiosk.tv/.MainActivity
  ```

Algunas marcas (por ejemplo TCL Google TV) además piden permiso de autoarranque:
```bash
adb shell appops set com.openkiosk.tv APP_AUTO_START allow
adb shell appops set com.openkiosk.tv SYSTEM_ALERT_WINDOW allow
```

Para deshacerlo: desinstala OpenKiosk o elige otra app de inicio en los ajustes de la TV.

### ¿Aún no tienes página de bienvenida?

Sirve cualquier página web. Una buena muestra: nombre del huésped, check-in/out, WiFi con código QR, reglas de la casa y cómo contactarte. **No pongas códigos de puerta ni de caja de llaves en la pantalla**: la página se puede abrir por su URL, así que trátala como información pública.

### Notas de seguridad

- Solo se permiten páginas `https://`; `http` simple se bloquea.
- La navegación se limita al dominio configurado y sus subdominios.
- El acceso a archivos desde la vista web está desactivado y las ventanas emergentes se bloquean.
- Pon un PIN. Sin PIN, cualquiera con acceso ADB a la TV podría cambiar la URL.

### Licencia

[GPL-3.0](LICENSE). Puedes usar, estudiar, compartir y modificar OpenKiosk gratis. Si distribuyes una versión modificada, debe seguir siendo de código abierto bajo la misma licencia.
