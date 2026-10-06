#!/bin/bash


BASE_URL="${BASE_URL:-http://localhost:8087/api/v1}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@biblioteca.com}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"
USER_EMAIL="${USER_EMAIL:-prueba.$(date +%s).$$@biblioteca.com}"
USER_PASS="${USER_PASS:-Lector123*}"

for herramienta in curl jq; do
  if ! command -v "$herramienta" >/dev/null 2>&1; then
    echo "Error: necesitas instalar $herramienta para ejecutar esta prueba."
    exit 1
  fi
done

if ! curl -s --connect-timeout 5 -o /dev/null "$BASE_URL/libros"; then
  echo "Error: no se puede conectar a $BASE_URL. Inicia la aplicación en el puerto 8087."
  exit 1
fi

echo "    INICIANDO PRUEBAS FUNCIONALES Y ESTRÉS"



# 1. REGISTRO Y LOGIN (Obtención de JWT)

echo -e "\n[1] Registrando usuario LECTOR de prueba..."
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'"$USER_EMAIL"'",
    "password": "'"$USER_PASS"'"
  }' | jq .

echo -e "\n[2] Autenticando usuario ADMIN..."
ADMIN_LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'"$ADMIN_EMAIL"'",
    "password": "'"$ADMIN_PASS"'"
  }')

ADMIN_TOKEN=$(printf '%s' "$ADMIN_LOGIN_RESP" | jq -r '.token // .accessToken')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo " --> Error al obtener el token de ADMIN. Revisa credenciales o endpoint /auth/login."
  exit 1
fi

echo "  Token Admin Obtenido: ${ADMIN_TOKEN:0:20}..."


# 2. PRUEBAS DE ENDPOINTS


echo -e "\n[3] Creando un nuevo libro (Rol ADMIN)..."
NUEVO_LIBRO_RESP=$(curl -s -X POST "$BASE_URL/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "titulo": "Effective Java 3rd Edition",
    "autor": "Joshua Bloch",
    "categoria": "Programación",
    "stockTotal": 10
  }')

printf '%s' "$NUEVO_LIBRO_RESP" | jq .
LIBRO_ID=$(printf '%s' "$NUEVO_LIBRO_RESP" | jq -r '.libroId')
if [ "$LIBRO_ID" == "null" ] || [ -z "$LIBRO_ID" ]; then
  echo "Error al crear el libro. Revisa la respuesta anterior."
  exit 1
fi

echo -e "\n[4] Consultando catálogo de libros (Autenticado)..."
curl -s -X GET "$BASE_URL/libros" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .


# 3. PRUEBA DE CONTROL DE ACCESO (403 Forbidden)


echo -e "\n[5] Autenticando usuario LECTOR..."
USER_LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'"$USER_EMAIL"'",
    "password": "'"$USER_PASS"'"
  }')

USER_TOKEN=$(printf '%s' "$USER_LOGIN_RESP" | jq -r '.token // .accessToken')
if [ "$USER_TOKEN" == "null" ] || [ -z "$USER_TOKEN" ]; then
  echo "Error al obtener el token de LECTOR."
  exit 1
fi

echo -e "\n[6] Intentando crear libro con Rol LECTOR (Debe fallar con 403 Forbidden)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "titulo": "Libro Prohibido",
    "autor": "Anon",
    "categoria": "Test",
    "stockTotal": 1
  }')

if [ "$HTTP_STATUS" -eq 403 ]; then
  echo "--> Seguridad Validada: Recibido Status 403 Forbidden correctamente."
else
  echo "--> Advertencia: Se esperaba 403 pero se obtuvo Status $HTTP_STATUS."
  exit 1
fi


# 4. PRUEBA DE ESTRÉS Y CONCURRENCIA

echo -e "\n"
echo "-->  EJECUTANDO PRUEBA DE ESTRÉS (CONCURRENCIA)"
echo " "

# Opción A: Si apachebench (ab) está instalado
if command -v ab &> /dev/null; then
  echo "Ejecutando 500 peticiones concurrentes (50 hilos) al catálogo de libros..."
  ab -n 500 -c 50 -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE_URL/libros"
else
  # Opción B: Fallback mediante cURL y xargs en paralelo
  echo "ApacheBench no encontrado. Usando cURL en paralelo (100 peticiones en 10 hilos)..."
  seq 100 | xargs -P 10 -I {} curl -s -o /dev/null -w "%{http_code}\n" \
    -X GET "$BASE_URL/libros" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | sort | uniq -c
fi

echo -e "\n"
echo " --> PRUEBAS COMPLETADAS"
echo " "
