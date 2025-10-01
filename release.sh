#!/bin/bash

# FlowMachine Release Script
# Usage: ./release.sh <version>
# Example: ./release.sh 1.0.0

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1

echo "🚀 Preparing release $VERSION..."

# Update version in pom.xml files
echo "📝 Updating version in pom.xml files..."
mvn versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false

# Run tests to ensure everything works
echo "🧪 Running tests..."
mvn clean test

# Build the project
echo "🔨 Building project..."
mvn clean package -DskipTests

# Create git tag
echo "🏷️  Creating git tag v$VERSION..."
git add .
git commit -m "Release version $VERSION" || echo "No changes to commit"
git tag -a "v$VERSION" -m "Release version $VERSION"

echo "✅ Release $VERSION prepared!"
echo ""
echo "Next steps:"
echo "1. Push to GitHub: git push origin main --tags"
echo "2. GitHub Actions will automatically create the release"
echo "3. JitPack will build the artifacts when first requested"
echo ""
echo "📦 To use this version in your project:"
echo "<dependency>"
echo "    <groupId>com.github.rayenrejeb</groupId>"
echo "    <artifactId>flow-machine</artifactId>"
echo "    <version>$VERSION</version>"
echo "</dependency>"