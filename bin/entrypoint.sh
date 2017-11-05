#!/bin/sh
set -e

echo Starting Wish application

echo Port is ${PORT}

/opt/app/bin/app -Dhttp.port=${PORT}
