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
- **Property name**
- **Host PIN** (exactly 6 digits, required)
- **Reload interval** (minutes, `0` = never)
- **Extra allowed domains** (optional, e.g. a CDN your page needs for navigation)

Or set it from a computer, without the remote:
```bash
adb shell am start -n com.openkiosk.tv/.MainActivity \
  --es url "https://your-site.com/?house=1" \
  --es name "Beach House 4B" \
  --es set_pin 123456 \
  --es reload_minutes 60
```
Once a PIN exists, changes by ADB require `--es pin <current PIN or authenticator code>`. The recovery code and the authenticator are set up on the TV screen, because they must be seen to be saved.

**Open settings later:** press **BACK 5 times**, or hold **OK** for 3 seconds, or press **MENU**. A short OK opens the guest apps.

### Guest flow

| Moment | What happens |
|---|---|
| TV turns on | Welcome screen |
| Guest presses **OK** | An elegant launcher with the streaming apps the host allowed |
| 5 min on the launcher without opening an app | Back to the welcome screen |
| An app is paused or silent for 10 min | Back to the welcome screen (needs the permission below) |
| Something is playing | Never interrupted |

Choose the apps in Settings → **Guest apps** (if you choose none, the usual streaming apps that are installed are shown). Both times are configurable.

To detect pauses, OpenKiosk needs a one-time permission. Android only allows this to a "notification listener", so on Google TV grant it from a computer:
```bash
adb shell cmd notification allow_listener com.openkiosk.tv/com.openkiosk.tv.MediaWatcherService
```
**Privacy:** OpenKiosk only reads whether something is playing (yes/no). It never reads titles, shows or notifications, and nothing is stored or sent.

For a guest who falls asleep while an app keeps auto-playing, also turn on the TV's own *auto power off after inactivity*.

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
- The PIN is stored as a salted PBKDF2 hash, never in plain text. Settings screens block screenshots.
- **Turn off network ADB after setup**, or keep the TV on a network separate from the guest WiFi. Otherwise any guest with a laptop could try to connect to it.

### Host PIN and recovery

OpenKiosk is used rarely by the host, so forgetting the PIN is expected. There are four ways back in, from most to least convenient:

| Method | You need | Notes |
|---|---|---|
| Authenticator app (TOTP) | Your phone | Scan a QR once when you create the PIN. Its 6-digit code also opens settings instead of the PIN. Works offline. |
| Recovery code | The 12-character code shown once | Save it in your password manager. Single use; a new one is generated after using it. |
| Signed reset from your welcome provider | Access to your provider | Planned for v0.3. |
| Clear app data with ADB | A computer already authorized on that TV | `adb shell pm clear com.openkiosk.tv`, then configure again. |

Wrong attempts lock the screen progressively: 5 errors = 1 min, then 5 min, 15 min and 1 hour. The lock survives reboots. There is no master PIN and no unsigned remote reset, on purpose.

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
- **Nombre de la propiedad**
- **PIN del anfitrión** (exactamente 6 dígitos, obligatorio)
- **Recarga** (minutos, `0` = nunca)
- **Dominios extra permitidos** (opcional)

O desde una computadora, sin el control:
```bash
adb shell am start -n com.openkiosk.tv/.MainActivity \
  --es url "https://tu-sitio.com/?casa=1" \
  --es name "Casa de Playa 4B" \
  --es set_pin 123456 \
  --es reload_minutes 60
```
Con PIN ya configurado, los cambios por ADB requieren `--es pin <PIN actual o código del autenticador>`. El código de recuperación y el autenticador se configuran en la pantalla de la TV, porque hay que verlos para guardarlos.

**Abrir ajustes después:** presiona **ATRÁS 5 veces**, o mantén **OK** 3 segundos, o presiona **MENÚ**. Un OK corto abre las apps del huésped.

### Experiencia del huésped

| Momento | Qué pasa |
|---|---|
| Se enciende la TV | Pantalla de bienvenida |
| El huésped presiona **OK** | Un launcher elegante con las apps de streaming que permitió el anfitrión |
| 5 min en el launcher sin abrir app | Regresa a la bienvenida |
| Una app queda en pausa o sin sonido 10 min | Regresa a la bienvenida (requiere el permiso de abajo) |
| Se está reproduciendo algo | Nunca se interrumpe |

Elige las apps en Ajustes → **Apps del huésped** (si no eliges ninguna, se muestran las apps de streaming conocidas que estén instaladas). Ambos tiempos se pueden cambiar.

Para detectar pausas, OpenKiosk necesita un permiso único. Android solo lo permite a un "servicio de notificaciones", así que en Google TV se concede desde una computadora:
```bash
adb shell cmd notification allow_listener com.openkiosk.tv/com.openkiosk.tv.MediaWatcherService
```
**Privacidad:** OpenKiosk solo lee si algo se está reproduciendo (sí/no). Nunca lee títulos, series ni notificaciones, y no guarda ni envía nada.

Para el huésped que se duerme mientras una app sigue reproduciendo sola, activa también el *apagado automático por inactividad* de la propia TV.

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
- El PIN se guarda como hash PBKDF2 con sal, nunca en texto plano. Las pantallas de ajustes bloquean capturas.
- **Apaga el ADB por red al terminar de configurar**, o deja la TV en una red separada del WiFi de huéspedes. Si no, cualquier huésped con una laptop podría intentar conectarse.

### PIN del anfitrión y recuperación

El anfitrión usa OpenKiosk muy de vez en cuando, así que olvidar el PIN es normal. Hay cuatro formas de volver a entrar, de la más cómoda a la de último recurso:

| Método | Necesitas | Notas |
|---|---|---|
| App autenticadora (TOTP) | Tu teléfono | Escaneas un QR una vez al crear el PIN. Su código de 6 dígitos también abre los ajustes en lugar del PIN. Funciona sin internet. |
| Código de recuperación | El código de 12 caracteres que se muestra una vez | Guárdalo en tu gestor de contraseñas. Es de un solo uso; al usarlo se genera otro. |
| Reset firmado desde tu proveedor de bienvenida | Acceso a tu proveedor | Planeado para la v0.3. |
| Borrar datos de la app con ADB | Una computadora ya autorizada en esa TV | `adb shell pm clear com.openkiosk.tv` y vuelves a configurar. |

Los intentos fallidos bloquean de forma progresiva: 5 errores = 1 min, luego 5 min, 15 min y 1 hora. El bloqueo sobrevive a reinicios. No hay PIN maestro ni reseteo remoto sin firma, a propósito.

### Licencia

[GPL-3.0](LICENSE). Puedes usar, estudiar, compartir y modificar OpenKiosk gratis. Si distribuyes una versión modificada, debe seguir siendo de código abierto bajo la misma licencia.
