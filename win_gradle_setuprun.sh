sh RefreshEnv.sh &&
choco install gradle -y &&
echo "openjdk8" &&
choco install jdk8 -y &&
refreshenv &&
./gradlew build &&
echo "openjdk10" &&
choco uninstall jdk8 -y &&
choco install zulu10 -y &&
refreshenv &&
./gradlew build