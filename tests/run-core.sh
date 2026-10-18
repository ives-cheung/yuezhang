#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/core-checks
javac -d build/core-checks app/src/main/java/com/ives/money/CarBrand.java app/src/main/java/com/ives/money/LedgerEntry.java app/src/main/java/com/ives/money/LedgerRules.java app/src/main/java/com/ives/money/BillParser.java tests/LedgerCoreChecks.java
java -ea -cp build/core-checks com.ives.money.LedgerCoreChecks

python3 tests/check-migration.py
