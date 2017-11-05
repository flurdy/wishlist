#!/bin/sh
set -e

echo Starting Wish application

/opt/app/bin/app -Dhttp.port=${PORT}
