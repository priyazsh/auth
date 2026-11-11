# Run locally

Java 25, Node, PostgreSQL, and `openssl`. Database `auth_db`, user `auth_user`. Set `DB_PASSWORD` (the file default is `change_me`).

From `server/`, create the ES256 key pair. The private key has to be PKCS#8.

```bash
mkdir -p config
openssl ecparam -name prime256v1 -genkey -noout -out /tmp/es256.pem
openssl pkcs8 -topk8 -nocrypt -in /tmp/es256.pem -out config/es256-private.pem
openssl ec -in /tmp/es256.pem -pubout -out config/es256-public.pem
rm /tmp/es256.pem
```

Google redirect URI: `http://localhost:8080/oauth2/callback`.

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export DB_PASSWORD=...
./mvnw spring-boot:run
```

The API is on port 8080. Health check: `http://localhost:8080/actuator/health`.

```bash
cd client
cp .env.example .env.local
```

Put `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` in `.env.local`, then `npm install` and `npm run dev`. The login page is `http://localhost:3000`.

For prod, set `spring.profiles.active=prod` and provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, the JWT key paths, the Google client, and the GoLink client through the environment. Apply `server/src/main/resources/schema.sql` first. Prod does not create tables.