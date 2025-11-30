# Release Process

This document describes how to create and publish a new release of the OpenSCAD IntelliJ Plugin.

## Prerequisites

### 1. JetBrains Marketplace Token

To publish to the JetBrains Marketplace, you need an API token:

1. Go to https://plugins.jetbrains.com/author/me/tokens
2. Click "Generate New Token"
3. Give it a name (e.g., "GitHub Actions")
4. Copy the token

### 2. Add Token to GitHub Secrets

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `JETBRAINS_MARKETPLACE_TOKEN`
5. Value: Paste your token from step 1
6. Click **Add secret**

## Creating a Release

### Automatic Release (Recommended)

The CI/CD pipeline automatically builds and publishes releases when you create a version tag:

```bash
# 1. Update version in build.gradle.kts (optional - CI will update plugin.xml)
# version = "1.0.0"

# 2. Commit any pending changes
git add .
git commit -m "Prepare for release v1.0.0"
git push

# 3. Create and push a version tag
git tag v1.0.0
git push origin v1.0.0
```

### What Happens Automatically

When you push a tag (e.g., `v1.0.0`), the GitHub Actions workflow will:

1. ✅ **Extract version** from tag (e.g., `v1.0.0` → `1.0.0`)
2. ✅ **Update plugin.xml** with the version number
3. ✅ **Build the plugin** (`buildPlugin`)
4. ✅ **Verify compatibility** (`verifyPlugin`)
5. ✅ **Create GitHub Release** with:
   - Plugin ZIP file attached
   - Auto-generated release notes
6. ✅ **Publish to JetBrains Marketplace** (if token is configured)

### Manual Release

If you prefer to release manually:

```bash
# 1. Update version in plugin.xml
# <version>1.0.0</version>

# 2. Build the plugin
./gradlew buildPlugin

# 3. Verify the plugin
./gradlew verifyPlugin

# 4. Publish to marketplace (requires PUBLISH_TOKEN env var)
export PUBLISH_TOKEN="your-token-here"
./gradlew publishPlugin

# 5. Create GitHub release manually
# Upload build/distributions/openscad-plugin-1.0.0.zip
```

## Version Numbering

Follow [Semantic Versioning](https://semver.org/):

- **Major** (1.0.0): Breaking changes
- **Minor** (0.1.0): New features, backwards compatible
- **Patch** (0.0.1): Bug fixes

Examples:
- `v0.1.0` - Initial beta release
- `v1.0.0` - First stable release
- `v1.1.0` - Added new features
- `v1.1.1` - Bug fixes

## Release Checklist

Before creating a release:

- [ ] All tests pass (`./gradlew test`)
- [ ] Plugin builds successfully (`./gradlew buildPlugin`)
- [ ] Plugin verified (`./gradlew verifyPlugin`)
- [ ] CHANGELOG.md updated with changes
- [ ] README.md is up to date
- [ ] All changes committed and pushed
- [ ] Version number follows semantic versioning

## Updating CHANGELOG

Create or update `CHANGELOG.md`:

```markdown
# Changelog

## [1.0.0] - 2025-11-30

### Added
- 3D preview with STL rendering
- Syntax highlighting for OpenSCAD
- Code completion and navigation
- Run configurations with animation support
- Batch export functionality

### Fixed
- Parser support for module declarations without braces
- Apple Silicon (ARM64) compatibility with JOGL 2.4.0

### Changed
- Updated IDE compatibility to support 2025.x builds

## [0.1.0] - 2025-11-29

### Added
- Initial release
- Basic OpenSCAD language support
```

## Troubleshooting

### Release Workflow Fails

**Problem:** "JETBRAINS_MARKETPLACE_TOKEN not found"
- **Solution:** Add the token to GitHub Secrets (see Prerequisites)

**Problem:** "Plugin verification failed"
- **Solution:** Run `./gradlew verifyPlugin` locally to see errors
- Check IDE compatibility in `build.gradle.kts`

**Problem:** "Marketplace upload failed"
- **Solution:** Check token permissions
- Verify plugin.xml has all required fields
- Check JetBrains Marketplace status page

### Version Mismatch

**Problem:** Version in plugin doesn't match tag
- **Solution:** The CI automatically updates plugin.xml from the tag
- Or manually update `<version>` in plugin.xml before tagging

### Plugin Not Appearing in Marketplace

**Problem:** Published but not visible
- **Solution:** Wait 1-3 business days for JetBrains approval
- Check email for approval/rejection notification
- First release requires manual approval

## Release Channels

The plugin supports different release channels:

- **Stable** (default): Production releases
- **Beta**: Pre-release testing
- **EAP**: Early access program

To publish to a different channel, update `build.gradle.kts`:

```kotlin
publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
    channels.set(listOf("beta"))  // or "eap"
}
```

## Post-Release

After a successful release:

1. ✅ Verify GitHub release created
2. ✅ Check JetBrains Marketplace listing
3. ✅ Test installation from marketplace
4. ✅ Announce on social media/forums
5. ✅ Monitor for issues/feedback

## Resources

- **GitHub Releases**: https://github.com/l2trace99/openscad-plugin/releases
- **JetBrains Marketplace**: https://plugins.jetbrains.com/
- **Plugin Portal**: https://plugins.jetbrains.com/author/me
- **Token Management**: https://plugins.jetbrains.com/author/me/tokens
- **Publishing Guide**: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html
