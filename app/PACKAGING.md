# CozyNote Packaging

## Prerequisites

- JDK 21 with `jpackage` on `PATH`
- Gradle on `PATH`, or a Gradle wrapper added later
- Windows installer builds require WiX Toolset. App-image builds do not.

## Build Commands

Create a runnable distribution:

```powershell
gradle clean installDist
```

Create a Windows app image:

```powershell
gradle packageWinImage
```

Create a Windows MSI installer:

```powershell
gradle packageWinInstaller
```

Or use the helper script:

```powershell
.\scripts\package-windows.ps1
```

## Checklist Before Packaging

- App icon: place `app.ico` at `src/main/resources/com/cozynote/assets/icons/app.ico`
- App name: `CozyNote`
- Version: `0.1.0`
- Save path: `%USERPROFILE%\.cozynote\cozynote.db`
- Basic features work: create note, edit block, auto save, search, settings, backup
- Required resources are included under `src/main/resources`
- Music files are optional and should not block note saving
- Test app image on Windows before making an installer

## Outputs

- Gradle distribution: `build/install/cozynote`
- jpackage output: `build/jpackage`
