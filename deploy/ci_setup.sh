#!/bin/bash

set -u

docker rm --force libats_slick-mariadb || true

mkdir libats_slick_entrypoint.d/ || true

echo "
CREATE DATABASE libats_slick;
GRANT ALL PRIVILEGES ON \`libats_slick%\`.* TO 'libats_slick'@'%';
FLUSH PRIVILEGES;
" > libats_slick_entrypoint.d/db_user.sql

MYSQL_PORT=${MYSQL_PORT-3306}

docker run -d \
  --name libats_slick-mariadb \
  -l service=libats \
  -p $MYSQL_PORT:3306 \
  -v $(pwd)/libats_slick_entrypoint.d:/docker-entrypoint-initdb.d \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_USER=libats_slick \
  -e MYSQL_PASSWORD=libats_slick \
  mariadb:10.1 \
  --character-set-server=utf8 --collation-server=utf8_unicode_ci \
  --max_connections=1000

startKafka() {
  docker run -d -p 2181:2181 -l service=libats --name zookeeper wurstmeister/zookeeper

  docker run -d -p 9092:9092 -l service=libats \
    --link zookeeper \
    -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 -e KAFKA_ADVERTISED_HOST_NAME=127.0.0.1 \
    -v /var/run/docker.sock:/var/run/docker.sock \
    wurstmeister/kafka:0.10.0.1
}

function mysqladmin_alive {
    docker run \
           --rm \
           --link libats_slick-mariadb \
           mariadb:10.1 \
           mysqladmin ping --protocol=TCP -h libats_slick-mariadb -P 3306 -u root -proot
}

TRIES=60
TIMEOUT=1s

startKafka

for t in `seq $TRIES`; do
    res=$(mysqladmin_alive || true)
    if [[ $res =~ "mysqld is alive" ]]; then
        echo "mysql is ready"
        exit 0
    else
        echo "Waiting for mariadb"
        sleep $TIMEOUT
    fi
done

exit -1
