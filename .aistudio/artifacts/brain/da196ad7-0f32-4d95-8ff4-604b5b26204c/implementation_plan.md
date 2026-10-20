# Plan de Implementación: Creación 1 a 1 de Bots y Personajes Estilo Character.ai con Texto Ilimitado

Este plan detalla la reestructuración completa de la interfaz y el flujo de creación de personajes, transformando la experiencia en un equivalente **1 a 1 de Character.ai**. Se eliminan todos los límites artificiales de caracteres y se simplifican los campos de creación tanto para Bots (PNJ/Narrador) como para Personajes del Jugador (Personas), integrando además la personalización de estadísticas RPG directamente desde el chat mediante comandos rápidos.

---

## 1. Interfaz de Creación de Bots (1 a 1 Character.ai)

En lugar de múltiples formularios fragmentados, el diálogo de creación y edición de Bots adoptará la estructura limpia oficial de Character.ai:

1. **Avatar Circular**:
   - Selector fotográfico con vista previa circular e icono de galería.
2. **Nombre del Personaje (`Name`)**:
   - Nombre de presentación del Bot/Personaje.
3. **Lema Descriptivo (`Tagline / Short Description`)**:
   - Breve frase de presentación que resume la identidad del bot (ej. *"Una hechicera elfa solitaria que custodia los secretos de un templo olvidado"*).
4. **Saludo Inicial (`Greeting / First Message`)**:
   - Primer mensaje de bienvenida con el que el bot inicia la aventura o se presenta ante el jugador.
5. **Definición Completa del Personaje (`Character Definition / Lore / Prompt`)**:
   - Campo unificado expansible **sin límite de caracteres** (se eliminan las restricciones previas de 50.000 caracteres, permitiendo prompts y trasfondos de cualquier longitud).
   - Compatible con formato narrativo libre o estructurado (`{{char}}: ... {{user}}: ...`).
6. **Música Ambiental Asociada**:
   - Selector colapsable minimalista de bandas sonoras y ocasiones musicales.

---

## 2. Creación de Personajes del Jugador (Personas de Character.ai)

Simplificación de la ficha del usuario al formato de **Persona de Character.ai**:

1. **Nombre de la Persona / Jugador**:
   - Nombre del protagonista con el que interactúa el narrador.
2. **Descripción de la Persona (`About Me / Persona`)**:
   - Descripción completa de aspecto, tono, clase, origen y debilidades **completamente ilimitada en longitud**.
3. **Avatar de la Persona**:
   - Imagen de perfil del jugador.
4. **Estadísticas RPG Personalizables por Chat**:
   - Ficha base compacta (Fuerza, Destreza, Constitución, Inteligencia, Sabiduría, Carisma, Puntos de Golpe).
   - **Comandos de Chat para Modificar Estadísticas en Tiempo Real**:
     - `/stat FUE 18` o `/stat STR 18` (establece la Fuerza a 18).
     - `/stat DES +2` o `/stat DEX +2` (incrementa la Destreza en 2).
     - `/hp 30` (actualiza los puntos de vida).
     - `/stats` (muestra la ficha de atributos del personaje activo en el chat).

---

## 3. Rediseño de la Lista de Personajes (Feed Estilo Character.ai)

- **Pestañas superiores**:
  - `Bots del Narrador`
  - `Mis Personajes (Personas)`
- **Tarjetas de Personaje C.AI**:
  - Avatar circular a la izquierda (`56.dp`).
  - Nombre en tipografía seminegrita con insignia visual si es el personaje activo.
  - Lema / Tagline en texto secundario suave con truncado inteligente.
  - Botón de acción minimalista: *"Interpretar Persona"* (si es de jugador) o menú de opciones discretas (Editar, Borrar).
- **Botón Flotante (`FAB`) de Creación**:
  - Botón "+ Crear" estilizado en la esquina inferior.

---

## 4. Archivos a Modificar

1. `app/src/main/java/com/example/ui/screens/characters/CharactersScreen.kt`:
   - Rediseño de `CharacterCard` al formato minimalista Character.ai.
   - Creación del nuevo `CharacterAiBotCreateSheet` (Nombre, Lema, Saludo, Definición Ilimitada, Avatar).
   - Creación del nuevo `CharacterAiPersonaCreateSheet` (Nombre, Descripción Ilimitada de Persona, Avatar, Ficha RPG).
2. `app/src/main/java/com/example/ui/viewmodel/RoleplayViewModel.kt`:
   - Agregar soporte para comandos de chat `/stat` y `/stats` para modificar atributos RPG en vivo durante la partida.
   - Guardado de personajes con texto ilimitado.
3. `app/src/main/java/com/example/ui/screens/chat/ChatScreen.kt`:
   - Incorporar sugerencias del comando `/stat` en la barra de autocompletado y atajos.

---

## 5. Plan de Verificación

1. **Compilación de la App (`compile_applet`)**:
   - Verificar que todos los nuevos componentes Compose, estados y parámetros compilen sin advertencias ni errores.
2. **Pruebas Unitarias (`gradle :app:testDebugUnitTest`)**:
   - Ejecutar la suite de tests unitarios y de Robolectric para validar la base de datos Room y la gestión de personajes.
3. **Validación Funcional**:
   - Crear un Bot con texto extenso (>50.000 caracteres) y verificar que no se trunque ni restrinja.
   - Crear una Persona de jugador con nombre y descripción ilimitada.
   - Probar en el chat el comando `/stat FUE 18` y verificar que la ficha de atributos se actualice inmediatamente.
