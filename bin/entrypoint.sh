#!/bin/sh
set -e

echo Starting Wish application

echo "Arguments are $*"

/opt/app/bin/app $* -Dhttp.port=${PORT}
