import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

// Dynamically locate mcmodding-mcp
const npmGlobal = process.env.APPDATA ? path.join(process.env.APPDATA, 'npm/node_modules/mcmodding-mcp') : '';
const mcmoddingDir = fs.existsSync(npmGlobal) ? npmGlobal : null;

if (!mcmoddingDir) {
  console.error('[RAG] Error: mcmodding-mcp not found in global npm modules.');
  process.exit(1);
}

const args = process.argv.slice(2);
const command = args[0] ? args[0].toLowerCase() : 'help';
const query = args.slice(1).join(' ');

async function main() {
  switch (command) {
    case 'docs':
    case 'doc':
    case 'search': {
      if (!query) {
        console.log('Usage: node scripts/rag.js search <query>');
        return;
      }
      const { SearchService } = await import(`file:///${mcmoddingDir.replace(/\\/g, '/')}/dist/services/search-service.js`);
      const searchService = new SearchService();
      const results = await searchService.search({ query, limit: 5 });
      console.log(`\n=== 📚 DOCS SEARCH RESULTS: "${query}" ===\n`);
      for (const res of results) {
        console.log(`📌 ${res.title} (Score: ${Math.round(res.score || 0)})`);
        if (res.url) console.log(`   URL: ${res.url}`);
        if (res.content) {
          const preview = res.content.slice(0, 300).replace(/\n+/g, ' ');
          console.log(`   Summary: ${preview}...\n`);
        }
      }
      break;
    }

    case 'class': {
      if (!query) {
        console.log('Usage: node scripts/rag.js class <className>');
        return;
      }
      const { MappingsService } = await import(`file:///${mcmoddingDir.replace(/\\/g, '/')}/dist/services/mappings-service.js`);
      if (!MappingsService.isAvailable()) {
        console.error('[RAG] parchment-mappings.db is not installed.');
        return;
      }
      const mappingsService = new MappingsService();
      const matches = mappingsService.search({ query, type: 'class', limit: 5 });
      console.log(`\n=== 🗺️ CLASS MAPPINGS: "${query}" ===\n`);
      for (const m of matches) {
        console.log(`🔷 ${m.name} (${m.fullName})`);
        console.log(`   Package: ${m.packageName}`);
        console.log(`   Obfuscated: ${m.obfuscatedName || 'N/A'}`);
        if (m.javadoc) console.log(`   Javadoc: ${m.javadoc}`);
        
        // If exact or close match, list key methods
        const cls = mappingsService.getClass(m.fullName);
        if (cls && cls.id) {
          const methods = mappingsService.getClassMethods(cls.id);
          console.log(`   Methods (${methods.length} total):`);
          const interesting = methods.slice(0, 10);
          for (const method of interesting) {
            const params = (method.parameters || []).map(p => `${p.name || 'arg'}`).join(', ');
            console.log(`     • ${method.name}(${params}) -> ${method.descriptor || 'void'}`);
          }
          if (methods.length > 10) {
            console.log(`     ... and ${methods.length - 10} more methods.`);
          }
        }
        console.log('');
      }
      break;
    }

    case 'method': {
      if (!query) {
        console.log('Usage: node scripts/rag.js method <methodName>');
        return;
      }
      const { MappingsService } = await import(`file:///${mcmoddingDir.replace(/\\/g, '/')}/dist/services/mappings-service.js`);
      if (!MappingsService.isAvailable()) {
        console.error('[RAG] parchment-mappings.db is not installed.');
        return;
      }
      const mappingsService = new MappingsService();
      const matches = mappingsService.search({ query, type: 'method', limit: 8 });
      console.log(`\n=== ⚡ METHOD MAPPINGS: "${query}" ===\n`);
      for (const m of matches) {
        const params = (m.parameters || []).map(p => `${p.name || 'arg'}`).join(', ');
        console.log(`🔹 ${m.className || 'Unknown'}.${m.name}(${params})`);
        console.log(`   Descriptor: ${m.descriptor || 'N/A'}`);
        console.log(`   Obfuscated: ${m.obfuscatedName || 'N/A'}`);
        if (m.javadoc) console.log(`   Javadoc: ${m.javadoc}`);
        console.log('');
      }
      break;
    }

    case 'project': {
      if (!query) {
        console.log('Usage: node scripts/rag.js project <query>');
        return;
      }
      console.log(`\n=== 🔍 LOCAL PROJECT SEARCH: "${query}" ===\n`);
      const searchDirs = ['common', 'fabric', 'neoforge'];
      let foundCount = 0;
      for (const d of searchDirs) {
        const dirPath = path.join(projectRoot, d);
        if (!fs.existsSync(dirPath)) continue;
        
        function scan(current) {
          const items = fs.readdirSync(current, { withFileTypes: true });
          for (const item of items) {
            const fullPath = path.join(current, item.name);
            if (item.isDirectory()) {
              if (item.name !== 'build' && item.name !== '.gradle') scan(fullPath);
            } else if (item.name.endsWith('.java') || item.name.endsWith('.json')) {
              const content = fs.readFileSync(fullPath, 'utf8');
              const lines = content.split('\n');
              const queryLower = query.toLowerCase();
              lines.forEach((line, idx) => {
                if (line.toLowerCase().includes(queryLower)) {
                  foundCount++;
                  const rel = path.relative(projectRoot, fullPath);
                  console.log(`  ${rel}:${idx + 1} -> ${line.trim()}`);
                }
              });
            }
          }
        }
        scan(dirPath);
      }
      console.log(`\nFound ${foundCount} matches in project.\n`);
      break;
    }

    case 'help':
    default:
      console.log(`
🧠 VillagerAI RAG Query Tool
=======================================
Usage:
  node scripts/rag.js search <query>    Search Fabric & NeoForge documentation with embeddings
  node scripts/rag.js class <name>      Search Minecraft classes, methods, and Javadocs
  node scripts/rag.js method <name>     Search Minecraft method signatures and descriptors
  node scripts/rag.js project <query>   Fast search across common, fabric, and neoforge sources

Examples:
  node scripts/rag.js class Villager
  node scripts/rag.js method setMemory
  node scripts/rag.js search "custom brain behavior"
  node scripts/rag.js project "AcquireNearestJobSite"
`);
      break;
  }
}

main().catch(err => {
  console.error('[RAG] Error:', err);
  process.exit(1);
});

