---
name: minecraft-rag
description: >-
  Fast RAG and retrieval system for Minecraft 26.3 modding, Fabric/NeoForge APIs, Mixin targets, Parchment mappings, and Villager AI mechanics. Use whenever you need to look up Minecraft classes, method signatures, behaviors, Brain memories, or modding guides without guessing or digging through Gradle caches.
---

# Minecraft & VillagerAI Fast RAG Retrieval

This project includes a high-speed RAG and retrieval system powered by `mcmodding-mcp` (vector embeddings + Parchment mappings + Fabric/NeoForge documentation) and local repository indexing.

## Available Retrieval Tools

You can execute queries instantly using `node scripts/rag.js` in a single command turn:

### 1. Lookup Minecraft Classes & Javadocs
To inspect Minecraft internals, methods, fields, and bytecode descriptors:
```bash
node scripts/rag.js class <ClassName>
# Examples:
node scripts/rag.js class Villager
node scripts/rag.js class PoiManager
node scripts/rag.js class Brain
node scripts/rag.js class MemoryModuleType
```

### 2. Lookup Method Signatures & Descriptors
To get parameter names, return types, and obfuscated names (essential for Mixins):
```bash
node scripts/rag.js method <MethodName>
# Examples:
node scripts/rag.js method setMemory
node scripts/rag.js method findClosest
node scripts/rag.js method registerBrainGoals
```

### 3. Semantic & FTS Search Across Fabric/NeoForge Documentation
Searches official Fabric & NeoForge documentation with neural embeddings and FTS:
```bash
node scripts/rag.js search "<question or topic>"
# Examples:
node scripts/rag.js search "custom brain behavior"
node scripts/rag.js search "mixin inject overwrite"
node scripts/rag.js search "poi type registration"
```

### 4. Search Local Project Code
Fast symbol and text search across `common`, `fabric`, and `neoforge` modules:
```bash
node scripts/rag.js project "<query>"
# Examples:
node scripts/rag.js project "AcquireNearestJobSite"
node scripts/rag.js project "VillagerGoalPackagesMixin"
node scripts/rag.js project "disableRoaming"
```

## Best Practices for the AI Agent
- **Never guess method signatures or obfuscated names**: Always check `node scripts/rag.js class <ClassName>` or `node scripts/rag.js method <MethodName>` first.
- **Never browse through giant Gradle decompiled caches**: The local Parchment database has 831K+ methods and 166K+ fields pre-indexed.
- **Check local project structure before making changes**: Core villager logic is in `common/`, platform entrypoints are in `fabric/` and `neoforge/`.

