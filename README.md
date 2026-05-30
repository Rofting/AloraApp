# Alora App - Asistente de Salud Integral

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)

Alora es una aplicación nativa para Android diseñada para facilitar la gestión de pacientes, el acceso rápido a información médica de emergencia y el seguimiento diario mediante bitácoras. Funciona en conjunto con un backend seguro desarrollado en Spring Boot.

## Funcionalidades Principales

* **Seguridad y Sesiones:** Autenticación de usuarios mediante tokens JWT.
* **Gestión de Pacientes:** Operaciones CRUD completas para perfiles de pacientes, incluyendo subida de fotografías desde la galería del dispositivo (`Multipart`).
* **QR de Emergencia:** Visualización de códigos QR únicos por paciente generados dinámicamente desde el servidor para acceso rápido a su ficha médica pública.
* **UX Moderna:** Borrado rápido de pacientes mediante gestos (*Swipe to delete*).
* **Bitácora del Cuidador (Care Logs):** Registro histórico de síntomas, medicación y notas generales del día a día del paciente.
* **Asistente IA (En desarrollo):** Interfaz conversacional preparada para integrar inteligencia artificial y reconocimiento de voz.

## Tecnologías Utilizadas (Tech Stack)

* **Lenguaje:** Java
* **Arquitectura / UI:** Activities, RecyclerView, CardView, Material Design.
* **Red y API:** [Retrofit2](https://square.github.io/retrofit/) y OkHttp3 para peticiones HTTP y manejo de archivos Multipart.
* **Imágenes:** [Glide](https://github.com/bumptech/glide) para carga asíncrona y caché de imágenes y QRs.
* **Almacenamiento Local:** SharedPreferences para persistencia del Token JWT.

## Instalación y Uso

Para probar este proyecto en local:

1. Clona el repositorio:
   ```bash
   git clone [https://github.com/Rofting/AloraApp.git](https://github.com/Rofting/AloraApp.git)
