# OpenSCAD Plugin Roadmap

## Current Status (v1.0.1)

The plugin provides core OpenSCAD language support with:
- ✅ Syntax highlighting
- ✅ Code intelligence (autocomplete, navigation)
- ✅ 3D preview with JOGL rendering
- ✅ Run configurations with animation support
- ✅ Export functionality (STL, PNG, multi-format, batch)
- ✅ File templates
- ✅ Library path configuration

## Known Issues

### High Priority

#### 3D Preview Colors and Grid (#TODO)
**Status:** Blocked by build/caching issue

**Problem:**
The 3D preview currently uses default JOGL colors (blue models, dark background) instead of OpenSCAD's distinctive color scheme (coral/orange models, light gray background). Additionally, there's no grid overlay to help with spatial orientation.

**Desired Behavior:**
- Light gray background (#E5E5E5) matching OpenSCAD
- Coral/orange object color (#F29D60) matching OpenSCAD
- Customizable horizontal grid centered on origin (default 250mm x 250mm)
- Grid settings in Settings → Tools → OpenSCAD

**Technical Details:**
Code changes have been implemented in:
- `STLViewer3D.kt` - Background color: `glClearColor(0.898f, 0.898f, 0.898f, 1.0f)`
- `STLViewer3D.kt` - Material colors: `matDiffuse = floatArrayOf(0.95f, 0.55f, 0.35f, 1.0f)`
- `STLViewer3D.kt` - Grid rendering with `drawGrid()` method
- `OpenSCADSettings.kt` - Grid configuration (size, spacing, visibility)
- `OpenSCADSettingsConfigurable.kt` - UI for grid settings

**Blocker:**
Despite code changes being present in source files and verified in compiled bytecode, the changes do not appear at runtime. This appears to be an IntelliJ plugin caching issue that persists across:
- Complete rebuilds (`./gradlew clean build`)
- Gradle daemon restarts (`./gradlew --stop`)
- IDE restarts
- Fresh plugin installations from ZIP
- Sandbox directory deletion

**Next Steps:**
1. Investigate IntelliJ plugin classloader behavior
2. Test in completely fresh environment (different machine/user)
3. Consider alternative approaches:
   - Use IntelliJ's dynamic plugin reloading
   - Implement as runtime configuration instead of compile-time
   - Use reflection to verify loaded class at runtime
4. Add debug logging to track which class version is being loaded

**Workaround:**
None currently. The preview works functionally but uses default JOGL colors.

---

## Planned Features

### Short Term (v1.1.x)

- [ ] **Syntax Error Highlighting** - Real-time error detection in editor
- [ ] **Code Formatting** - Auto-format OpenSCAD code
- [ ] **Live Preview Updates** - Update preview while typing (debounced)
- [ ] **Preview Camera Presets** - Quick camera angles (top, front, side, isometric)
- [ ] **Measurement Tools** - Measure distances in 3D preview
- [ ] **Cross-platform Testing** - Verify on Windows and Linux

### Medium Term (v1.2.x)

- [ ] **Customizer Support** - UI for OpenSCAD customizer parameters
- [ ] **STL Analysis** - Show model statistics (volume, surface area, etc.)
- [ ] **Code Snippets** - Common OpenSCAD patterns
- [ ] **Refactoring Support** - Rename variables, extract modules
- [ ] **Dependency Graph** - Visualize include/use relationships
- [ ] **Performance Profiling** - Identify slow rendering operations

### Long Term (v2.0.x)

- [ ] **Integrated Slicer** - Direct integration with slicing software
- [ ] **Version Control Integration** - Visual diff for 3D models
- [ ] **Collaborative Features** - Share and review designs
- [ ] **Cloud Rendering** - Offload heavy renders to cloud
- [ ] **AI Assistance** - Code suggestions and optimization
- [ ] **Mobile Preview** - View designs on mobile devices

---

## Community Requests

Track feature requests from users here. Create issues on GitHub for specific requests.

---

## Contributing

Want to help implement these features? See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

**Priority Areas:**
1. Fixing the 3D preview colors/grid issue
2. Cross-platform testing and bug fixes
3. Documentation improvements
4. Performance optimization

---

## Version History

### v1.0.1 (2025-11-30)
- Fixed file templates
- Added comprehensive plugin description
- Improved marketplace publishing workflow

### v1.0.0 (2025-11-30)
- Initial release
- Core language support
- 3D preview with JOGL
- Run configurations
- Export functionality

### v0.1.0 (2025-11-29)
- Beta release
- Basic OpenSCAD support
