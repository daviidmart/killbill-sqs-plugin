#!/bin/bash

# Configuración de variables
CONTAINER_NAME="service_subscriptions-killbill-1"
JAR_NAME="eventbridge-notification-plugin-1.0.0.jar"
PLUGIN_DIR="/var/lib/killbill/bundles/plugins/java/eventbridge-plugin/1.0.0"
OSGI_CACHE_DIR="/var/tmp/felix"

# Salir inmediatamente si un comando falla
set -e

echo "🚀 Iniciando proceso de despliegue..."

# 1. Compilación
echo "📦 Compilando el proyecto con Maven..."
mvn clean install -DskipTests

# 2. Limpieza de archivos previos y cache de OSGi
echo "🧹 Limpiando instalación previa y cache de Felix en Docker..."
docker exec -u root service_subscriptions-killbill-1 rm -rf /var/tmp/felix/*
docker exec -u root $CONTAINER_NAME rm -rf $OSGI_CACHE_DIR/*
docker exec -u root $CONTAINER_NAME rm -rf $PLUGIN_DIR/*
docker exec -u root $CONTAINER_NAME mkdir -p $PLUGIN_DIR

# 3. Subir el nuevo archivo
echo "📤 Copiando el nuevo JAR al contenedor..."
docker cp target/$JAR_NAME $CONTAINER_NAME:$PLUGIN_DIR/eventbridge-plugin.jar

# 4. Corregir permisos
echo "🔑 Ajustando permisos para el usuario tomcat..."
docker exec -u root $CONTAINER_NAME chown -R tomcat:tomcat /var/lib/killbill/bundles/plugins/java/eventbridge-plugin/

# 5. Reiniciar Kill Bill (Crucial para limpiar memoria OSGi)
echo "🔄 Reiniciando el contenedor de Kill Bill..."
docker restart $CONTAINER_NAME

echo "✅ ¡Despliegue completado con éxito!"
echo "📡 Revisa los logs con: docker logs -f $CONTAINER_NAME"