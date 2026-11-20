# Helios

## Getting Started

This repository includes external Git submodules (such as [GeoidHeight4J](https://github.com/Aknbb/geoidheight4j) for geoid altitude corrections).

### Clone with Submodules

To clone the repository along with its submodules:

```bash
git clone --recurse-submodules https://github.com/Val0rous/Helios.git
```

### If You Already Cloned Without Submodules

Initialize and pull the submodules from the project root:

```bash
git submodule update --init --recursive
```

### Optional Git Convenience Settings

To automatically update initialized submodules on future `git pull` / `git checkout` commands:

```bash
git config --global submodule.recurse true
```

To add a shorthand alias for recursive clones on your machine:

```bash
git config --global alias.cloner "clone --recurse-submodules"
```

### Updating Submodules

To pull the latest commit from a tracked submodule and record it in this repository:

```bash
git submodule update --remote app/src/main/java/geoidheight4j
git add app/src/main/java/geoidheight4j
git commit -m "Update geoidheight4j submodule"
```
