#!/bin/bash

# Файл build.sh для сборки и запуска Telegram Approval Bot в фоновом режиме

# Останавливаем и удаляем предыдущие контейнеры (если есть)
echo "Остановка предыдущих контейнеров..."
docker-compose down -v

# Собираем и запускаем контейнеры в фоновом режиме
echo "Сборка и запуск контейнеров в фоне..."
docker-compose up --build

# Проверяем статус контейнеров
echo "Проверка состояния контейнеров..."
docker-compose ps

# Выводим логи для проверки (первые 50 строк)
echo "Последние логи сервиса bot:"
docker-compose logs --tail=50 bot

echo "Последние логи сервиса db:"
docker-compose logs --tail=20 db

echo ""
echo "Сервисы успешно запущены в фоновом режиме!"
echo "Для просмотра логов используйте: docker-compose logs -f"
echo "Для остановки: docker-compose down"