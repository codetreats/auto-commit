import simpleGit, { SimpleGit, StatusResult } from 'simple-git';
import * as fs from 'fs-extra';
import * as path from 'path';
import { randomUUID } from 'crypto';
import { appendFileSync } from "fs";

interface Change {
    path: string;
    type: 'modified' | 'added' | 'deleted';
    oldFile?: string;
    newFile?: string;
}

interface RepoStatus {
    status: 'UPTODATE' | 'CHANGED' | 'UNTRACKED' | 'UNPUSHED';
    category: string;
    repo: string;
    path: string;
    timestamp: string;
    changes: Change[];
}

interface SummaryEntry {
    category: string;
    repo: string;
    status: string;
    changesCount: number;
    jsonFile: string;
}

const GIT_BASE_DIR = '/git';
const OUTPUT_DIR = '/var/www/html/diffs';

async function ensureOutputDir(): Promise<void> {
    await fs.ensureDir(OUTPUT_DIR);
}

async function saveFileWithUUID(content: string): Promise<string> {
    const uuid = randomUUID();
    const filename = `${uuid}.txt`;
    const filepath = path.join(OUTPUT_DIR, filename);
    await fs.writeFile(filepath, content, 'utf-8');
    return filename;
}

async function getFileContent(repoPath: string, filePath: string, ref: string = 'HEAD'): Promise<string | null> {
    try {
        const git: SimpleGit = simpleGit(repoPath);
        const content = await git.show([`${ref}:${filePath}`]);
        return content;
    } catch (error) {
        return null;
    }
}

async function getCurrentFileContent(repoPath: string, filePath: string): Promise<string | null> {
    try {
        const fullPath = path.join(repoPath, filePath);
        if (await fs.pathExists(fullPath)) {
            return await fs.readFile(fullPath, 'utf-8');
        }
        return null;
    } catch (error) {
        return null;
    }
}

async function isBinaryFile(repoPath: string, filePath: string): Promise<boolean> {
    try {
        const fullPath = path.join(repoPath, filePath);
        if (!(await fs.pathExists(fullPath))) {
            return false;
        }
        const buffer = await fs.readFile(fullPath);
        for (let i = 0; i < Math.min(buffer.length, 8000); i++) {
            if (buffer[i] === 0) {
                return true;
            }
        }
        return false;
    } catch (error) {
        return false;
    }
}

async function processChanges(repoPath: string, status: StatusResult): Promise<Change[]> {
    const changes: Change[] = [];
    
    const allFiles = [
        ...status.modified.map((f: string) => ({ path: f, type: 'modified' as const })),
        ...status.created.map((f: string) => ({ path: f, type: 'added' as const })),
        ...status.deleted.map((f: string) => ({ path: f, type: 'deleted' as const })),
        ...status.renamed.map((r: any) => ({ path: r.to, type: 'modified' as const }))
    ];

    for (const file of allFiles) {
        if (await isBinaryFile(repoPath, file.path)) {
            console.log(`  Skipping binary file: ${file.path}`);
            continue;
        }

        const change: Change = {
            path: file.path,
            type: file.type
        };

        if (file.type === 'modified' || file.type === 'deleted') {
            const oldContent = await getFileContent(repoPath, file.path, 'HEAD');
            if (oldContent !== null) {
                change.oldFile = await saveFileWithUUID(oldContent);
            }
        }

        if (file.type === 'modified' || file.type === 'added') {
            const newContent = await getCurrentFileContent(repoPath, file.path);
            if (newContent !== null) {
                change.newFile = await saveFileWithUUID(newContent);
            }
        }

        changes.push(change);
    }

    return changes;
}

async function analyzeRepo(category: string, repo: string): Promise<RepoStatus> {
    const repoPath = path.join(GIT_BASE_DIR, category, repo);
    
    const gitDir = path.join(repoPath, '.git');
    if (!(await fs.pathExists(gitDir))) {
        return {
            status: 'UNTRACKED',
            category,
            repo,
            path: repoPath,
            timestamp: new Date().toISOString(),
            changes: []
        };
    }

    const git: SimpleGit = simpleGit(repoPath);
    
    try {
        const status: StatusResult = await git.status();
        
        const hasUncommittedChanges = 
            status.modified.length > 0 ||
            status.created.length > 0 ||
            status.deleted.length > 0 ||
            status.renamed.length > 0;

        if (hasUncommittedChanges) {
            const changes = await processChanges(repoPath, status);
            return {
                status: 'CHANGED',
                category,
                repo,
                path: repoPath,
                timestamp: new Date().toISOString(),
                changes
            };
        }

        try {
            await git.fetch('origin', 'master');
            
            if (status.ahead > 0) {
                return {
                    status: 'UNPUSHED',
                    category,
                    repo,
                    path: repoPath,
                    timestamp: new Date().toISOString(),
                    changes: []
                };
            }
        } catch (error) {
            console.warn(`  Warning: Could not check remote status for ${category}/${repo}`);
        }

        return {
            status: 'UPTODATE',
            category,
            repo,
            path: repoPath,
            timestamp: new Date().toISOString(),
            changes: []
        };

    } catch (error) {
        throw new Error(`Failed to analyze repo ${category}/${repo}: ${error}`);
    }
}

async function scanAllRepos(): Promise<void> {
    console.log('Starting repository scan...');
    
    await ensureOutputDir();
    
    const summary: SummaryEntry[] = [];
    const categories = await fs.readdir(GIT_BASE_DIR);
    
    for (const category of categories) {
        const categoryPath = path.join(GIT_BASE_DIR, category);
        const stat = await fs.stat(categoryPath);
        
        if (!stat.isDirectory()) {
            continue;
        }

        console.log(`\nScanning category: ${category}`);
        logStep(process.argv[2], category)
        
        const repos = await fs.readdir(categoryPath);
        
        for (const repo of repos) {
            const repoPath = path.join(categoryPath, repo);
            const repoStat = await fs.stat(repoPath);
            
            if (!repoStat.isDirectory()) {
                continue;
            }

            console.log(`  Analyzing: ${category}/${repo}`);
            
            try {
                const repoStatus = await analyzeRepo(category, repo);
                
                const jsonFilename = `${category}_${repo}.json`;
                const jsonPath = path.join(OUTPUT_DIR, jsonFilename);
                await fs.writeJSON(jsonPath, repoStatus, { spaces: 2 });
                
                summary.push({
                    category,
                    repo,
                    status: repoStatus.status,
                    changesCount: repoStatus.changes.length,
                    jsonFile: jsonFilename
                });
                
                console.log(`    Status: ${repoStatus.status} (${repoStatus.changes.length} changes)`);
                
            } catch (error) {
                console.error(`\n❌ ERROR: Failed to process ${category}/${repo}`);
                console.error(`   ${error}`);
                process.exit(1);
            }
        }
    }
    
    // Summary.json speichern
    const summaryPath = path.join(OUTPUT_DIR, 'summary.json');
    await fs.writeJSON(summaryPath, summary, { spaces: 2 });
    
    console.log('\n✅ Repository scan completed successfully!');
    console.log(`   Total repositories: ${summary.length}`);
    console.log(`   With changes: ${summary.filter(s => s.status === 'CHANGED').length}`);
}


function logStep(statusFilePath: string, stepName: string): void {
  const dateTime = getLocalTimestamp();

  console.log("[STEP]");
  console.log("[STEP] #############################################");
  console.log(`[STEP] ${dateTime}: ${stepName}`);
  console.log("[STEP] #############################################");

  appendFileSync(statusFilePath, `${dateTime}: ${stepName}\n`, "utf8");
}

function getLocalTimestamp(): string {
  const d = new Date();
  const pad = (n: number) => n.toString().padStart(2, "0");

  return (d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()) + "_" + pad(d.getHours()) + ":" + pad(d.getMinutes()) + ":" + pad(d.getSeconds()));
}


scanAllRepos().catch(error => {
    console.error('\n❌ FATAL ERROR:');
    console.error(error);
    process.exit(1);
});