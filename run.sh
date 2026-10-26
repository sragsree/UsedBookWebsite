#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v mvn >/dev/null 2>&1; then
  echo 'Maven 3.9+ is required. Install it, then rerun ./run.sh.' >&2; exit 1
fi
if [[ "$(uname -s)" == Darwin && -x /usr/libexec/java_home ]]; then
  detected_java=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
  if [[ -n "$detected_java" && -z "${JAVA_HOME:-}" ]]; then export JAVA_HOME="$detected_java"; fi
fi
maven_args=(-s .mvn/settings.xml -f bookstore-angular/pom.xml -B)
if [[ -n "${BOOKSTORE_MAVEN_CACHE:-}" ]]; then maven_args+=("-Dmaven.repo.local=$BOOKSTORE_MAVEN_CACHE"); fi
case "${1:-run}" in
 test) exec mvn "${maven_args[@]}" test ;;
 build) exec mvn "${maven_args[@]}" clean verify ;;
 run) mvn "${maven_args[@]}" -DskipTests package
      java_command=java
      if [[ -n "${JAVA_HOME:-}" ]]; then java_command="$JAVA_HOME/bin/java"; fi
      exec "$java_command" -jar bookstore-angular/target/bookstore-angular-1.0.0.jar ;;
 *) echo 'Usage: ./run.sh [run|test|build]' >&2; exit 2 ;;
esac
