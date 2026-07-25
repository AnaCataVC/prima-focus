# Reglas de Proyecto: Prima-Focus

## 🚀 Sincronización del Showcase

Cuando se realicen actualizaciones importantes en este repositorio (como modificaciones sustanciales en la documentación de `/docs`, compilación de un nuevo APK, o la creación de un nuevo Release), **SIEMPRE debes actualizar también el repositorio público de exhibición (Showcase)** o, como mínimo, ofrecerte a hacerlo de inmediato.

El repositorio de exhibición (Showcase) se encuentra en la ruta local: `C:\Users\anaca\Repos\prima-focus-showcase`.
La URL del repositorio en GitHub es: `https://github.com/AnaCataVC/prima-focus-showcase`.

### Instrucciones para la actualización del Showcase:
1. **Documentación:** Si actualizas la documentación técnica en este repositorio (ej. esquemas de DB, arquitectura, notas de diseño), asegúrate de copiar los archivos modificados a la carpeta `docs/` del Showcase y subir los cambios (commit & push).
2. **Nuevos Releases / APKs:** 
   - **Versionado:** La nueva versión SIEMPRE debe calcularse contrastando la última versión publicada (tags) en el repositorio del Showcase, NO en el repositorio privado.
   - **Archivos (Compilación):** NUNCA subas un APK "unsigned" al Showcase porque no se puede instalar. Asegúrate de compilar la variante Release (`.\gradlew assembleRelease` en `app/android`), la cual ya está configurada para firmarse automáticamente. Copia el archivo generado (ej. `app-release.apk`) a la ruta `C:\Users\anaca\Repos\prima-focus-showcase\apk\` y renómbralo a algo profesional como `prima-focus-vX.Y.Z.apk`.
   - **Página del Showcase:** Recuerda siempre actualizar el número de versión en la página principal o `README.md` del Showcase.
   - **Publicación:** Actualiza el repositorio y genera un nuevo Release en GitHub para el Showcase (usando `gh release create`) con notas de versión detalladas.
3. **Privacidad del Código:** NUNCA copies el código fuente (`/app` ni la lógica de la aplicación) al Showcase. El Showcase está diseñado para ser público y contener únicamente: `README.md`, el archivo APK, el archivo LICENSE y la carpeta `docs/`.
