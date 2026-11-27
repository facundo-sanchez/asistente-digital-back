# Despliegue gratuito

- **Backend, Postgres y los documentos:** una máquina de Oracle Cloud Always Free, con Docker Compose y Caddy (HTTPS).
- **Front (Next.js):** Vercel, plan Hobby.
- **Emails:** Brevo (SMTP gratis).

El backend tiene tareas programadas (avisos, emails, Gmail cada 15 minutos), por eso va en una máquina que está siempre encendida y no en un servicio que se duerme sin tráfico.

## 1. La máquina (Oracle Cloud)

1. Creá la cuenta en Oracle Cloud y una instancia **Always Free**: Ubuntu 24.04, forma **VM.Standard.A1.Flex** (ARM) con 2 OCPU y 12 GB. Guardá la clave SSH.
2. En la red de la instancia (VCN → Security List), agregá reglas de entrada **TCP 80 y 443** desde `0.0.0.0/0`.
3. Entrá por SSH y abrí los puertos también en el firewall de Ubuntu:
   ```sh
   sudo iptables -I INPUT 6 -p tcp --dport 80 -j ACCEPT
   sudo iptables -I INPUT 6 -p tcp --dport 443 -j ACCEPT
   sudo netfilter-persistent save
   ```
4. Instalá Docker:
   ```sh
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER   # salí y volvé a entrar
   ```

## 2. El dominio del backend

Con [DuckDNS](https://www.duckdns.org) creás gratis un subdominio (ej. `api-asistente.duckdns.org`) y lo apuntás a la IP pública de la máquina. Caddy saca el certificado HTTPS solo al arrancar.

## 3. El backend

```sh
git clone <tu repo del backend> asistente-digital-backend
cd asistente-digital-backend/deploy

# Claves del JWT (nuevas, solo para producción)
mkdir -p secrets
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/jwt-private.pem
openssl rsa -in secrets/jwt-private.pem -pubout -out secrets/jwt-public.pem
chmod 644 secrets/*.pem   # el contenedor corre con otro usuario y tiene que leerlas

cp .env.prod.example .env.prod
nano .env.prod             # completá todo (ver comentarios)

docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml logs -f backend   # Flyway crea las tablas al arrancar
```

Prueba: `https://api-asistente.duckdns.org/asistente-digital-back/api/v1/auth/login` tiene que responder (un 405 o 400 por GET está bien: el backend contestó).

**Actualizar** después de un `git pull`: el mismo `up -d --build`.

**Backup diario de la base** (crontab -e):
```
0 4 * * * docker exec deploy-db-1 pg_dump -U asistente asistente_digital | gzip > ~/backups/db-$(date +\%F).sql.gz
```
Los documentos subidos viven en el volumen `deploy_documents`.

## 4. El front (Vercel)

1. En [vercel.com](https://vercel.com), **Add New → Project** e importá el repo del front. Detecta Next.js solo.
2. Variables de entorno del proyecto:
   | Variable | Valor |
   |---|---|
   | `API_BACKEND` | `https://api-asistente.duckdns.org/asistente-digital-back/api/v1` |
   | `NEXT_PUBLIC_BASE_PATH` | vacío para servir en la raíz, o `/asistente-digital` |
3. Deploy. Con cada push a la rama de producción se vuelve a publicar.
4. Ajustá `FRONTEND_URL` del backend a la URL de Vercel (con el basePath si lo pusiste) y reiniciá: `docker compose ... up -d`.

## 5. Google y Gmail

En Google Cloud Console → Credenciales → tu ID de cliente OAuth, sumá las URIs de redirección de producción:
- `FRONTEND_URL/api/auth/google/callback`
- `FRONTEND_URL/api/integrations/gmail/callback`

`gmail.readonly` es un permiso **restringido**: mientras la app esté en modo prueba, solo pueden conectar Gmail los usuarios de prueba que cargues (hasta 100). Para abrirlo a cualquiera, Google pide verificar la app y una auditoría de seguridad. El login con Google no tiene esa traba.

## 6. Antes de abrirla

- Claves nuevas para todo: JWT, `INTEGRATIONS_KEY`, base de datos, Google, Gemini.
- Gemini en el nivel gratuito: si el proyecto de la clave no tiene facturación, al llegar al tope responde error y no cobra.
- Brevo: verificá el remitente (`SMTP_FROM`) para que los emails no caigan en spam.
