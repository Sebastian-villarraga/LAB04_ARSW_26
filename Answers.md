# Laboratorio 4 - BluePrints

**Elaborado Por**

Sebastián Villarraga

Juan Carlos Leal Cruz

## **Actividades del Laboratorio**
### **Parte 1. Familiarización con el código base**
El proyecto está estructurado siguiendo un patrón de arquitectura en capas, lo que permite separar las responsabilidades de la aplicación de manera limpia y escalable.

1. **Capa Modelo**

   - `Point`: Implementado como un record de Java, lo que lo hace inmutable por defecto. Representa una coordenada bidimensional (x, y) dentro de un plano.
   - `Blueprint`: Representa el plano en sí. Contiene un autor (`author`), un nombre (`name`) y una lista de puntos (`List<Point>`). Se destaca el uso de `Collections.unmodifiableList(points)` en su método getter para proteger la integridad de los datos y evitar modificaciones externas no deseadas.

2. **Capa de Persistencia**
   - `BlueprintPersistence`: Una interfaz que define el contrato para las operaciones de base de datos (CRUD), aislando la lógica de negocio de la tecnología de almacenamiento.
   - `InMemoryBlueprintPersistence`: InMemoryBlueprintPersistence: Es la implementación actual (`@Repository`). Utiliza un `ConcurrentHashMap` para almacenar los planos en memoria. El uso de colecciones concurrentes es una excelente decisión arquitectónica aquí, ya que los beans de Spring son singletons por defecto y múltiples hilos (peticiones HTTP) podrían intentar leer o escribir datos simultáneamente.

3. **Capa de Servicios y Controlador**
   - `BlueprintsServices`: Anotada con `@Service`, esta clase actúa como intermediaria. Conecta el controlador con la persistencia. Un detalle arquitectónico importante es la inyección de `BlueprintsFilter` (aplicado en el método `getBlueprint`), lo que sugiere el uso de un patrón de diseño (como Strategy) para filtrar los puntos de un plano antes de entregarlos.
   - `BlueprintsAPIController`: Anotada con `@RestController`, expone los recursos a través de HTTP. Maneja adecuadamente los códigos de estado HTTP (ej. `201 CREATED` para POST, `202 ACCEPTED` para PUT, y `404 NOT FOUND` cuando se capturan excepciones personalizadas como `BlueprintNotFoundException`).

### **Parte 2. Migración a persistencia en PostgreSQL**
Como parte de la evolución arquitectónica de la API, se reemplazó el almacenamiento en memoria (`InMemoryBlueprintPersistence`) por una base de datos relacional PostgreSQL, garantizando la persistencia real de los datos entre reinicios de la aplicación.

A continuación se detalla el paso a paso de la implementación:
#### **1. Infraestructura con Docker**
Para aislar el entorno de base de datos y facilitar la ejecución del proyecto en cualquier máquina, se implementó un contenedor de Docker utilizando `docker-compose`.
Se creó el archivo `docker-compose.yml` en la raíz del proyecto, configurando la imagen oficial `postgres:15-alpine` y exponiendo el puerto `5432`.

**Comando para levantar la infraestructura:**
`docker compose up -d`

![DockerContainer.png](Images/DockerContainer.png)
![DockerEvidence.png](Images/DockerEvidence.png)

#### **2. Esquema de Base de Datos y Relaciones**
A diferencia de otros enfoques que requieren un archivo schema.sql manual, se aprovechó la capacidad de Hibernate para autogenerar el esquema relacional directamente desde el código Java.

Las relaciones se definieron a través de anotaciones:
- Tabla blueprints: Generada a partir de `BluePrintEntity`. Se definió un `@Id` autogenerado y una relación `@OneToMany` con los puntos, utilizando `CascadeType.ALL` para que al guardar un plano, sus puntos se persistan automáticamente. 
- Tabla points: Generada a partir de `PointEntity`. Contiene sus coordenadas y una relación `@ManyToOne` que crea automáticamente la llave foránea `blueprint_id` vinculándola a su plano correspondiente.


#### **3. Configuración de la Aplicación**
Se modificó el archivo `application.properties` para configurar las credenciales de conexión al contenedor de Docker y se instruyó a Spring JPA para que actualice el esquema de la base de datos automáticamente (`ddl-auto=update):
```
spring.datasource.url=jdbc:postgresql://localhost:5432/blueprintsdb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

#### **4. Implementación del Repositorio**
Se implementó la migración en dos capas:
- **Spring Data Repository:** Se creó la interfaz `BluePrintRepository` que hereda de JPA, obteniendo métodos CRUD automáticos y consultas personalizadas (ej. `findByAuthorAndName`).
- **Nueva Persistencia (`PostgresBlueprintPersistence`):** Se creó una nueva clase que implementa la interfaz original `BlueprintPersistence`.
  - Inyección de Dependencias: Se utilizó la anotación `@Primary` en esta clase. Gracias a esto, el contenedor de inversión de control (IoC) de Spring le da prioridad sobre la versión en memoria, inyectándola automáticamente en los servicios sin modificar la lógica de negocio existente.
  - Mapeo de Datos: Se implementó un método interno `mapToModel()` encargado de traducir las entidades extraídas de la base de datos (`BluePrintEntity`) a los objetos de dominio puros (`Blueprint`) requeridos por el contrato de la interfaz.

#### **6. Pruebas de Funcionamiento**
Se realizón un POST y un GET básicos para comprobar que en efecto funciona la base de datos PostgreSQL haciendo uso de Postman:
- **POST**
  ![Post.png](Images/Post.png)
- **GET**
  ![Get.png](Images/Get.png)

Luego de esto, se procedió a consultar como tal la terminal para así estar seguros de la utilización de esta base de datos:
![DBTerminal.png](Images/DBTerminal.png)

### **Parte 3. Buenas prácticas de API REST**
#### **1. Versionado y Enrutamiento Semántico**
Se modificó el path base de los controladores para incluir el contexto de la aplicación y el versionado de la API. Este proceso fue realizado en el archivo `BlueprintsAPIController.java`
- Nuevo Path: `/api/v1/blueprints`
- Justificación: El prefijo `/api` separa claramente las rutas de datos de cualquier posible contenido estático, mientras que `/v1` (Versión 1) establece un contrato inicial. Esto permite que en el futuro se puedan introducir cambios drásticos (lanzando un /v2) sin romper las aplicaciones de los clientes que siguen consumiendo la versión anterior.

#### **2. Códigos de Estado HTTP Precisos**
Se reemplazó el uso genérico de respuestas con un manejo estricto y semántico de los códigos de estado HTTP, informando al cliente exactamente qué ocurrió con su petición:
- `200 OK`: Utilizado para consultas exitosas (ej. obtener todos los planos o un plano específico).
- `201 Created`: Devuelto exclusivamente cuando un nuevo plano es guardado exitosamente en la base de datos a través de un `POST`.
- `202 Accepted`: Implementado para indicar que una solicitud de actualización (ej. agregar un nuevo punto a un plano existente) fue recibida y procesada correctamente.
- `400 Bad Request`: Utilizado cuando el cliente envía un cuerpo de petición mal formado o con datos inválidos (ej. un punto sin coordenadas).
- `404 Not Found`: Lanzado mediante el manejo de excepciones (`BlueprintNotFoundException`) cuando se solicita un autor o plano que no existe en los registros de PostgreSQL.

#### **4. Estandarización de Respuestas**
Para evitar que los clientes consuman estructuras de datos inconsistentes (a veces un objeto, a veces una lista, a veces un mensaje de texto puro), se implementó un contenedor genérico utilizando la característica de inmutabilidad de los Records de Java. Esto se realizó haciendo uso de:
```
public record ApiResponseWrapper<T>(
        int code,
        String message,
        T data
) {}
```

De tal forma que el usuario ve la siguiente información:
- `code`: Refleja el código HTTP o un código de negocio interno.
- `message`: Provee un mensaje descriptivo en lenguaje natural (ej. "Blueprint creado exitosamente" o "Autor no encontrado").
- `data`: Un campo genérico (`T`) que transporta el payload real (puede ser un único `Blueprint`, una lista de ellos, o null en caso de error).

Aquí hay un ejemplo de cómo lo ve el usuario al momento de un `POST`:
![CodeEv.png](Images/CodeEv.png)

### **Parte 4. Documentación Automática con OpenAPI / Swagger**
Para garantizar que la API sea descubrible y mantenga un contrato claro entre el Backend y cualquier posible cliente, se implementó la especificación OpenAPI. Esta herramienta genera "documentación viva" que se actualiza automáticamente conforme el código evoluciona.

#### **1. Integración con `springdoc-openapi`**
Se incluyó la dependencia `springdoc-openapi-starter-webmvc-ui` (versión 2.6.0) en el archivo `pom.xml.
- Justificación: A diferencia de mantener documentación en archivos de texto estáticos que rápidamente quedan obsoletos, esta librería inspecciona el código en tiempo de ejecución de Spring Boot 3 y genera la especificación OpenAPI dinámicamente. Esto reduce la carga de mantenimiento y asegura que la documentación sea siempre la fuente de información verídica.

#### **2. Configuración Global y Metadatos**
Se creó la clase `OpenApiConfig` para definir los metadatos principales del servicio mediante un `@Bean` de tipo OpenAPI.
- Justificación: Permite establecer el contexto del proyecto, definiendo el título ("ARSW Blueprints API"), la versión ("v1") y la información de contacto de los autores. Esto es fundamental cuando la API se expone a equipos externos o se publica en un portal de desarrolladores.

#### **3. Enriquecimiento Semántico de Endpoints**
Para que la interfaz gráfica sea realmente útil, se decoró el controlador `BlueprintsAPIController` con anotaciones específicas que explican el comportamiento del sistema:
- Agrupación (`@Tag`): Se definió a nivel de clase para agrupar bajo el nombre "Blueprints API" todas las operaciones relacionadas con los planos arquitectónicos.
- Descripciones (`@Operation`): Se utilizó en cada método (ej. `summary = "Create a new blueprint"`) para describir claramente qué hace el endpoint y qué se espera de él.
- Contratos de Respuesta (`@ApiResponses` y `@ApiResponse`): Se mapearon los posibles códigos HTTP de respuesta definidos en el paso anterior (200 OK, 201 Created, 202 Accepted, 404 Not Found) junto con su respectiva descripción. Esto le indica al cliente exactamente qué estructura (el `ApiResponseWrapper`) va a recibir.
- Ejemplos Interactivos (`@Parameter` y `@Schema`): Se enriquecieron las variables de ruta y los cuerpos de petición (`NewBlueprintRequest`) con ejemplos concretos (ej. `author = "john", name = "house"`). Esto facilita enormemente el uso de la interfaz de pruebas.

#### **4. Interfaz Interactiva (Swagger UI)**
Toda esta configuración se materializa en una interfaz gráfica accesible desde el navegador.
- Acceso: Una vez levantada la aplicación, la documentación interactiva está disponible en la ruta `/swagger-ui/index.html` (o `/swagger-ui.html` como redirección).
![Swagger.png](Images/Swagger.png)

### **Parte 5. Filtrado de Datos**
Para optimizar la transferencia de datos y adaptar la respuesta de la API según las necesidades del cliente (por ejemplo, dispositivos con recursos limitados o redes lentas), se implementó un mecanismo de filtrado sobre los puntos que componen un plano arquitectónico.
En lugar de modificar la lógica central del servicio (`BlueprintsServices`) llenándola de condicionales `if/else`, se delegó esta responsabilidad aplicando el patrón de diseño Estrategia soportado por los perfiles de Spring Boot.

#### **1. La interfaz de Filtrado**
Se definió la interfaz `BlueprintsFilter` con un único método `apply(Blueprint bp)`. Esto establece un contrato claro: cualquier filtro que se cree en el futuro debe recibir un plano, procesarlo y devolver un plano resultante, garantizando que el servicio principal no necesite saber cómo se filtran los datos.

#### **2. Implementación de los Filtros**
Se desarrollaron tres implementaciones distintas de la interfaz para manejar diferentes escenarios de negocio:
- `RedundancyFilter: Itera sobre la lista de puntos y elimina cualquier punto que sea idéntico a su predecesor inmediato.
  
  **Justificación:** Es ideal para limpiar "ruido" en los datos, como cuando un usuario en el Frontend hace doble clic por error en la misma coordenada al dibujar.
- `UndersamplingFilter`: Reduce la densidad del plano conservando únicamente 1 de cada 2 puntos (los índices pares), omitiendo los impares.

  **Justificación:** Funciona como un mecanismo de compresión (downsampling) muy útil cuando se deben renderizar planos excesivamente grandes y detallados en dispositivos móviles o conexiones de bajo ancho de banda.
- `IdentityFilter`: Devuelve el plano exactamente igual, sin alteraciones.

  **Justificación:** Actúa como un comportamiento por defecto seguro, garantizando que si no se requiere ningún filtro específico, la aplicación no falle al intentar inyectar la dependencia.

#### **3. Activación Dinámica mediante Spring Profiles**
El aspecto más potente de esta implementación es cómo se decide qué filtro utilizar en tiempo de ejecución utilizando la anotación `@Profile` sobre los componentes (`@Component`). Esto permite cambiar el comportamiento de toda la API simplemente modificando la configuración de arranque, sin recompilar el código Java:
- `@Profile("redundancy")`: Le indica al contenedor de Spring (IoC) que inyecte el `RedundancyFilter` únicamente si el perfil "redundancy" está activo.
- `@Profile("undersampling")`: Inyecta el `UndersamplingFilter` solo si el perfil "undersampling" está activo.
- `@Profile("!redundancy & !undersampling")`: Es una expresión lógica que garantiza que el `IdentityFilter` sea inyectado como opción predeterminada cuando ninguno de los otros dos perfiles haya sido activado explícitamente.

**¿Cómo probarlo**

Para activar uno de estos filtros al levantar la aplicación, basta con modificar el archivo `application.properties` agregando la siguiente línea:
```
spring.profiles.active=redundancy     #Dependiendo del perfil a usar
```

