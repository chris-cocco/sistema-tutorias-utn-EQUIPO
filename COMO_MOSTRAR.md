# Cómo mostrar el proyecto en la exposición

Guía rápida para levantar cada parte del sistema el día de la exposición.
Pruébalo todo la noche antes o esa misma mañana, no en el momento.

## Sitio web

No hay que correr nada localmente, ya está desplegado:

```
https://sistema-tutorias-utn-equipo.onrender.com
```

Es un plan gratuito de Render que "se duerme" tras un rato sin visitas y
tarda ~30 segundos en despertar. **Ábrelo 5-10 minutos antes de exponer.**

Credenciales de prueba están en el [README](README.md).

## App de teléfono (Android)

1. Abre Android Studio → abre la carpeta `app_android`.
2. Arriba, elige el dispositivo **Telefono_Pruebas** y la configuración **app**.
3. Dale ▶ (Run).

## Reloj (Wear OS)

1. Mismo Android Studio, carpeta `app_android` ya abierta.
2. Elige el dispositivo **Reloj_Pruebas** y la configuración **wear**.
3. Dale ▶ (Run). Debe abrir en su propia ventana (no en el panel
   embebido de Android Studio, que se ha congelado antes).
4. Al abrir la app va a pedir el permiso de notificaciones — dale "Allow".
5. Toca "Ver sensor" para mostrar el acelerómetro leyendo datos reales.

### Si el emulador se congela (pantalla fija que no reacciona a nada)

Es un bug conocido del renderizado de Android Studio en Windows, no del
proyecto. Ciérralo así:

1. Cierra Android Studio por completo.
2. Ábrelo de nuevo y vuelve a intentar el paso anterior.
3. Si sigue sin responder, en `Tools → Emulator` confirma que
   **"Launch in the Running Devices tool window"** esté desmarcado, para
   que el emulador abra en ventana aparte en vez de empotrado en la IDE.
