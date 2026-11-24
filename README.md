🔵 MICROSERVICIO: USUARIOS SERVICE

Responsabilidad: Gestión de usuarios, roles y sesiones.

Tablas
1. usuarios
   Campo	Tipo	Descripción
   id	UUID	PK
   nombre	VARCHAR
   apellido	VARCHAR
   email	VARCHAR UNIQUE
   rol	VARCHAR	(CIUDADANO, AGENTE, SUPERVISOR)
   activo	BOOLEAN
   fecha_alta	TIMESTAMP
2. credenciales (opcional si no usás Keycloak)
   Campo	Tipo	Descripción
   usuario_id	UUID	FK a usuarios
   hash_password	TEXT
   salt	TEXT
   🟦 MICROSERVICIO: SOLICITUDES SERVICE (Core)
   Tablas
1. solicitudes
   Campo	Tipo	Descripción
   id	UUID	PK
   titulo	VARCHAR
   descripcion	TEXT
   fecha_creacion	TIMESTAMP
   fecha_cierre	TIMESTAMP NULL
   estado_id	INT	FK a estado_solicitud
   tipo_id	INT	FK a tipo_solicitud
   prioridad	VARCHAR	(BAJA, MEDIA, ALTA)
   usuario_creador_id	UUID	FK a usuarios-service
   usuario_asignado_id	UUID NULL	FK a usuarios-service

La referencia al usuario es por ID, pero sin foreign key directa (porque es otro microservicio).

2. estado_solicitud

Tabla catálogo:

| id | INTEGER PK |
| nombre | VARCHAR |
| descripcion | VARCHAR |
| orden | INT |

3. tipo_solicitud

Tabla catálogo:

| id | INTEGER |
| nombre | VARCHAR |
| descripcion | VARCHAR |

4. (Opcional) historial_estados

| id | UUID |
| solicitud_id | UUID |
| estado_anterior | INT |
| estado_nuevo | INT |
| fecha | TIMESTAMP |
| usuario_id | UUID |

🟩 MICROSERVICIO: MENSAJES SERVICE
Tablas
1. mensajes_solicitud
   Campo	Tipo	Descripción
   id	UUID	PK
   solicitud_id	UUID	ID de Solicitudes Service
   usuario_id	UUID	ID de Usuarios Service
   mensaje	TEXT
   fecha_envio	TIMESTAMP
2. adjuntos

| Campo | Tipo | Descripción |
| id | UUID |
| mensaje_id | UUID NULL |
| solicitud_id | UUID NULL |
| nombre_archivo | VARCHAR |
| mime_type | VARCHAR |
| url | VARCHAR |
| fecha_subida | TIMESTAMP |

🟥 MICROSERVICIO: NOTIFICACIONES SERVICE
Tablas
1. notificaciones_enviadas

| Campo | Tipo | Descripción |
| id | UUID |
| correo_destino | VARCHAR |
| tipo | VARCHAR | (CREACION, CIERRE, ALERTA) |
| asunto | VARCHAR |
| cuerpo | TEXT |
| fecha_envio | TIMESTAMP |
| estado | VARCHAR | (ENVIADO, ERROR) |

2. plantillas_email

| Campo | Tipo |
| id | INT |
| nombre | VARCHAR |
| plantilla | TEXT |
| descripcion | VARCHAR |

🟧 MICROSERVICIO: DOCUMENTOS / PDF SERVICE
Tablas
1. pdf_generados

| Campo | Tipo |
| id | UUID |
| solicitud_id | UUID |
| url_pdf | VARCHAR |
| fecha_generacion | TIMESTAMP |
| tipo_documento | VARCHAR | (RESUMEN, CIERRE, REPORTE_MENSUAL) |

🟫 MICROSERVICIO: AUDITORÍA SERVICE
Tablas
1. auditoria_eventos

| Campo | Tipo |
| id | UUID |
| servicio | VARCHAR | (solicitudes, mensajes, usuarios, etc.) |
| entidad | VARCHAR |
| entidad_id | UUID |
| accion | VARCHAR | (CREAR, ACTUALIZAR, ELIMINAR) |
| payload | JSONB |
| fecha | TIMESTAMP |
| usuario_id | UUID |