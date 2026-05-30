## Alora App

  <p align="center">
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
    <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
    <img src="https://img.shields.io/badge/Retrofit-48B983?style=for-the-badge&logo=square&logoColor=white"/>
    <img src="https://img.shields.io/badge/Room-4285F4?style=for-the-badge&logo=sqlite&logoColor=white"/>
    <img src="https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=materialdesign&logo
  Color=white"/>
    <img src="https://img.shields.io/badge/Gemini%20AI-8E75B2?style=for-the-badge&logo=google&logoColor=white"/>
  </p>

  <p align="center">
    Aplicación Android nativa de gestión de cuidados geriátricos con asistente de inteligencia artificial,
  recordatorios médicos y acceso de emergencia por QR.
  </p>

  <p align="center">
    <b>Proyecto de Fin de Grado · Desarrollo de Aplicaciones Multiplataforma</b>
  </p>

  ---

  ## Descripción

  Alora está pensada para cuidadores de personas mayores. Permite gestionar fichas médicas completas, registrar
  la evolución diaria del paciente, programar alarmas de medicación y consultar un asistente de inteligencia
  artificial por voz o texto.

  La app funciona en modo **offline-first**: Room devuelve los datos en caché de forma inmediata mientras
  Retrofit sincroniza con el servidor en segundo plano.

  ---

  ## Funcionalidades

  **Gestión de pacientes**
  - Alta, edición y borrado de perfiles médicos
  - Subida de fotografía desde galería (Android Photo Picker)
  - Borrado mediante gesto swipe
  - Caché offline con Room y sincronización automática en segundo plano

  **Autenticación**
  - Registro e inicio de sesión con JWT
  - Redirección automática a Login cuando el token expira
  - Nombre del usuario logueado visible en la cabecera

  **Bitácora clínica**
  - Registro de síntomas, medicación y notas diarias
  - Edición y eliminación con confirmación
  - Paginación de registros

  **Recordatorios y alarmas**
  - Programación por hora y días de la semana
  - Notificación al paciente con apertura automática del asistente IA
  - Síntesis de voz al dispararse la alarma
  - Las alarmas se reprograman automáticamente tras reiniciar el dispositivo (WorkManager)

  **Asistente IA (Gemini 2.5 Flash)**
  - Conversación por voz o texto en español
  - Se activa de forma proactiva al recibir una alarma de medicación
  - Interpreta lenguaje natural del paciente:
    - "Ya me la tomé" → marca el recordatorio como completado
    - "Ahora no puedo" / "Estoy acostada" → pospone 30 minutos y reprograma la alarma
    - "Recuérdame la pastilla a las 10" → crea el recordatorio directamente
  - Guarda todas las interacciones en la bitácora

  **Tarjeta vital de emergencia**
  - Código QR único por paciente generado por el servidor
  - Accesible sin autenticación para uso en emergencias
  - Desbloqueo de datos médicos completos mediante PIN verificado en servidor
  - Muestra alergias, condiciones, medicación activa y contacto de emergencia

  ---

  ## Tech Stack

  | Capa | Tecnología |
  |---|---|
  | Lenguaje | Java 11 |
  | UI | Activities · RecyclerView · Material Design 3 |
  | Red | Retrofit 2.11 · OkHttp 4.12 |
  | Persistencia local | Room 2.6.1 |
  | Imágenes | Glide 4.16 |
  | Background | WorkManager 2.9 |
  | QR | ZXing Android Embedded |
  | Min SDK | 28 (Android 9) |
  | Target SDK | 36 (Android 15) |

  ---

  ## Arquitectura

  La app sigue un patrón **Activity-based offline-first** sin ViewModel ni MVVM. La capa de datos está separada
  en tres fuentes:

  - **Retrofit** gestiona las peticiones al backend y expone los endpoints definidos en `ApiService`
  - **Room** cachea la lista de pacientes localmente para funcionamiento sin conexión
  - **WorkManager** ejecuta tareas en segundo plano como la reprogramación de alarmas tras reinicio

  El token JWT se almacena en SharedPreferences mediante `TokenManager`. Un interceptor de OkHttp en `ApiClient`
  detecta respuestas 401 en peticiones autenticadas y redirige automáticamente a la pantalla de login.

  ui/          Activities y Adapters
  api/         Retrofit client y definición de endpoints
  model/       Modelos de datos y DTOs
  local/       Room database y DAOs
  util/        TokenManager · AlarmHelper · AlarmReschedulerWorker

  ---

  ## Instalación

  Requisitos: Android Studio Hedgehog o superior · JDK 11 · El
  [backend](https://github.com/Rofting/alora-backend) corriendo localmente.

  **1. Clona el repositorio**

  ```bash
  git clone https://github.com/Rofting/AloraApp.git

  2. Configura la URL del servidor

  Abre app/src/main/java/com/alora/app/api/ApiClient.java y ajusta BASE_URL según dónde corra el backend:

  // Emulador Android
  public static final String BASE_URL = "http://10.0.2.2:8080/";

  // Dispositivo físico en red local
  public static final String BASE_URL = "http://192.168.X.X:8080/";

  3. Abre el proyecto en Android Studio

  File → Open → selecciona la carpeta del proyecto

  4. Ejecuta la app

  Selecciona un emulador o dispositivo físico con API 28 o superior y pulsa Run.

  ---
  Backend

  Este proyecto requiere el servidor alora-backend para funcionar:

  https://github.com/Rofting/alora-backend

  Stack: Spring Boot 3.5 · Java 21 · PostgreSQL · Flyway · JWT · Gemini 2.5 Flash
  ```
