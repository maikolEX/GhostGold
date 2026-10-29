# GHOST GOLD

Launcher negro y dorado para la **Dongfeng Rich 6 2026** de Maikol, en su radio NWD (Allwinner T3, Android 7.1, 1280×720). App propia, escrita desde cero.

**La camioneta es el centro.** Estacionado se ve de frente sobre un pedestal dorado; tras dos minutos sin tocar la pantalla entra en *showroom* y gira. Al pasar de 8 km/h se pone de espaldas sobre la carretera, con las líneas avanzando y las llantas girando a la velocidad real.

## Qué tiene

- **Inicio**: hora, saludo, clima (Open-Meteo), restricción vehicular de San José, velocidad GPS, odómetro, radio, música y alertas alrededor de la camioneta.
- **Arranque**: sello GG, *GHOST GOLD · SYSTEM ONLINE · VEHICLE READY* y la camioneta aparece encendiendo las luces. Al encender el radio y con cada encendido del carro.
- **Radio**: frecuencia grande, cinta de sintonía tipo avión (arrastrar para sintonizar), búsqueda y emisoras guardadas.
- **Teléfono**: estado del Bluetooth, favoritos, recientes y teclado dorado.
- **Mantenimiento**: aceite, frenos, rotación, filtros, marchamo y RTV, con avisos.
- **Resumen del viaje** al apagar el carro.
- **Panel sobre Android Auto** (ZLink): velocidad, restricción, mantenimiento y alertas; se arrastra y se minimiza.
- **Volumen dorado**: anillo con el nivel al usar los botones del volante.
- **Noche**: reloj dorado tenue.

## Compilar

GitHub Actions compila cada cambio en `main` y publica el APK en
`https://github.com/maikolEX/GhostGold/releases/download/latest/GhostGold.apk`.

En una computadora: `ANDROID_HOME=/ruta/al/sdk sh build.sh` (necesita JDK 17, `platforms;android-33` y build-tools).

Las tipografías Michroma y Barlow (SIL Open Font License) se descargan durante la compilación.
La llave `ci/release.keystore` firma todas las versiones para que se instalen una encima de otra.
