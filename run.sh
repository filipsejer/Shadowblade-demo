#!/bin/sh
# Compiles and launches the game. Needs a JDK (17+).
#
#   ./run.sh                normal launch
#   ./run.sh safe           conservative launch: software rendering (no Metal/OpenGL), memory capped at 512 MB,
#                           output saved to game.log, and any JVM crash report written to ./hs_err_pid*.log
#   ./run.sh silent         launch with all sound switched off (the sound engine is never started)
#   ./run.sh notutorial     the title screen starts with the opening story switched off (T on the title screen toggles it anyway)
#   ./run.sh safe silent    both
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -d out src/game/*.java || exit 1

SAFE=0
OPTS=""
for arg in "$@"; do
    case "$arg" in
        safe) SAFE=1 ;;
        silent) OPTS="$OPTS -Dspellblade.audio=off" ;;
        notutorial) OPTS="$OPTS -Dspellblade.tutorial=off" ;;
    esac
done

if [ "$SAFE" = "1" ]; then
    java -Dsun.java2d.metal=false -Dsun.java2d.opengl=false $OPTS \
         -Xmx512m -XX:ErrorFile=./hs_err_pid%p.log \
         -cp out game.Main 2>&1 | tee game.log
else
    java $OPTS -cp out game.Main
fi
