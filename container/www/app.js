let allRepos = [];
let selectedRepo = null;
let selectedFile = null;
let repoData = {};
let commitMessages = {};

// Beim Laden der Seite
document.addEventListener('DOMContentLoaded', async () => {
    await loadSummary();
    renderSidebar();
});

// Summary laden
async function loadSummary() {
    try {
        const response = await fetch('/diffs/summary.json');
        allRepos = await response.json();
        console.log('Loaded repos:', allRepos.length);
    } catch (error) {
        console.error('Failed to load summary:', error);
        alert('Fehler beim Laden der Repository-Liste');
    }
}

// Sidebar rendern
function renderSidebar() {
    const sidebar = document.getElementById('sidebar');
    sidebar.innerHTML = '';

    // Nach Kategorien gruppieren
    const categories = {};
    allRepos.forEach(repo => {
        if (!categories[repo.category]) {
            categories[repo.category] = [];
        }
        categories[repo.category].push(repo);
    });

    // Kategorien rendern
    Object.keys(categories).sort().forEach(category => {
        const categorySection = document.createElement('div');
        categorySection.className = 'category-section';
        
        const categoryHeader = document.createElement('div');
        categoryHeader.className = 'category-header';
        categoryHeader.innerHTML = `
            <span class="category-arrow">▼</span>
            <span>${category}</span>
        `;
        
        categoryHeader.addEventListener('click', () => {
            categorySection.classList.toggle('collapsed');
        });

        const repoList = document.createElement('div');
        repoList.className = 'repo-list';

        categories[category].forEach(repo => {
            const repoItem = document.createElement('div');
            repoItem.className = 'repo-item';
            
            if (repo.changesCount > 0) {
                repoItem.classList.add('has-changes');
            }

            const statusClass = repo.status.toLowerCase();
            
            repoItem.innerHTML = `
                <div class="status-icon ${statusClass}"></div>
                <div class="repo-name">${repo.repo}</div>
                ${repo.changesCount > 0 ? `<div class="changes-count">${repo.changesCount}</div>` : ''}
            `;

            repoItem.addEventListener('click', () => selectRepo(repo));

            repoList.appendChild(repoItem);

            // Commit Message Input für Repos mit Changes
            if (repo.changesCount > 0) {
                const messageBox = document.createElement('div');
                messageBox.className = 'commit-message-box';
                messageBox.innerHTML = `
                    <input 
                        type="text" 
                        placeholder="Commit message für ${repo.repo}..."
                        data-repo="${repo.category}/${repo.repo}"
                        value="${commitMessages[`${repo.category}/${repo.repo}`] || ''}"
                    />
                `;
                
                const input = messageBox.querySelector('input');
                input.addEventListener('input', (e) => {
                    commitMessages[`${repo.category}/${repo.repo}`] = e.target.value;
                    updateButtons();
                });

                repoList.appendChild(messageBox);
            }
        });

        categorySection.appendChild(categoryHeader);
        categorySection.appendChild(repoList);
        sidebar.appendChild(categorySection);
    });
}

// Repository auswählen
async function selectRepo(repo) {
    // Vorherige Auswahl entfernen
    document.querySelectorAll('.repo-item').forEach(item => {
        item.classList.remove('selected');
    });

    // Neue Auswahl markieren
    event.currentTarget.classList.add('selected');

    selectedRepo = repo;
    selectedFile = null;

    // Repo-Daten laden
    if (!repoData[repo.jsonFile]) {
        try {
            const response = await fetch(`/diffs/${repo.jsonFile}`);
            repoData[repo.jsonFile] = await response.json();
        } catch (error) {
            console.error('Failed to load repo data:', error);
            alert('Fehler beim Laden der Repository-Daten');
            return;
        }
    }

    const data = repoData[repo.jsonFile];

    // Spezielle Meldungen je nach Status
    if (data.status === 'UNTRACKED') {
        showEmptyState('Dieses Verzeichnis ist kein Git-Repository');
        return;
    }

    if (data.status === 'UPTODATE') {
        showEmptyState('Repository ist auf dem neuesten Stand');
        return;
    }

    if (data.status === 'UNPUSHED') {
        showEmptyState('Repository hat unpushed Commits');
        return;
    }

    if (data.changes.length === 0) {
        showEmptyState('Keine Änderungen in diesem Repository');
        return;
    }

    // File-Liste anzeigen
    renderFileList(data.changes);
    
    // Ersten File automatisch auswählen
    selectFile(data.changes[0]);
}

// File-Liste rendern
function renderFileList(changes) {
    const fileList = document.getElementById('fileList');
    const emptyState = document.getElementById('emptyState');
    
    emptyState.style.display = 'none';
    fileList.style.display = 'block';
    fileList.innerHTML = '';

    changes.forEach(change => {
        const fileTab = document.createElement('div');
        fileTab.className = 'file-tab';
        
        const typeLabel = change.type === 'modified' ? 'M' : 
                         change.type === 'added' ? 'A' : 'D';
        
        fileTab.innerHTML = `
            <span class="file-type ${change.type}">${typeLabel}</span>
            <span>${change.path}</span>
        `;

        fileTab.addEventListener('click', () => selectFile(change));

        fileList.appendChild(fileTab);
    });
}

// File auswählen
async function selectFile(change) {
    // Vorherige Auswahl entfernen
    document.querySelectorAll('.file-tab').forEach(tab => {
        tab.classList.remove('selected');
    });

    // Neue Auswahl markieren
    event.currentTarget.classList.add('selected');

    selectedFile = change;

    // Diff anzeigen
    await renderDiff(change);
}

// Diff rendern
async function renderDiff(change) {
    const diffViewer = document.getElementById('diffViewer');
    diffViewer.style.display = 'block';
    diffViewer.innerHTML = '';

    const header = document.createElement('div');
    header.className = 'diff-header';
    header.textContent = `${change.path} (${change.type})`;
    diffViewer.appendChild(header);

    if (change.type === 'added') {
        // Nur neue Datei anzeigen
        const newContent = await loadFileContent(change.newFile);
        renderSingleFile(diffViewer, newContent, 'added');
    } else if (change.type === 'deleted') {
        // Nur alte Datei anzeigen
        const oldContent = await loadFileContent(change.oldFile);
        renderSingleFile(diffViewer, oldContent, 'removed');
    } else {
        // Modified: Diff anzeigen
        const oldContent = await loadFileContent(change.oldFile);
        const newContent = await loadFileContent(change.newFile);
        renderUnifiedDiff(diffViewer, oldContent, newContent);
    }
}

// Datei-Inhalt laden
async function loadFileContent(filename) {
    if (!filename) return '';
    
    try {
        const response = await fetch(`/diffs/${filename}`);
        return await response.text();
    } catch (error) {
        console.error('Failed to load file:', error);
        return '';
    }
}

// Einzelne Datei rendern (für added/deleted)
function renderSingleFile(container, content, type) {
    const lines = content.split('\n');
    
    lines.forEach(line => {
        const lineDiv = document.createElement('div');
        lineDiv.className = `diff-line ${type}`;
        lineDiv.textContent = (type === 'added' ? '+ ' : '- ') + line;
        container.appendChild(lineDiv);
    });
}

// Unified Diff rendern
function renderUnifiedDiff(container, oldContent, newContent) {
    const oldLines = oldContent.split('\n');
    const newLines = newContent.split('\n');
    
    const diff = computeDiff(oldLines, newLines);
    
    diff.forEach(block => {
        // Header für den Block
        const headerDiv = document.createElement('div');
        headerDiv.className = 'diff-line header';
        headerDiv.textContent = `@@ -${block.oldStart},${block.oldCount} +${block.newStart},${block.newCount} @@`;
        container.appendChild(headerDiv);

        // Zeilen des Blocks
        block.lines.forEach(line => {
            const lineDiv = document.createElement('div');
            lineDiv.className = `diff-line ${line.type}`;
            
            const prefix = line.type === 'added' ? '+ ' : 
                          line.type === 'removed' ? '- ' : '  ';
            
            lineDiv.textContent = prefix + line.content;
            container.appendChild(lineDiv);
        });
    });
}

// Einfacher Diff-Algorithmus
function computeDiff(oldLines, newLines) {
    const blocks = [];
    let i = 0, j = 0;
    
    while (i < oldLines.length || j < newLines.length) {
        const block = {
            oldStart: i + 1,
            newStart: j + 1,
            oldCount: 0,
            newCount: 0,
            lines: []
        };

        // Context-Zeilen sammeln
        while (i < oldLines.length && j < newLines.length && oldLines[i] === newLines[j]) {
            block.lines.push({ type: 'context', content: oldLines[i] });
            i++;
            j++;
            block.oldCount++;
            block.newCount++;
        }

        // Unterschiede sammeln
        let hasChanges = false;
        const tempOld = [];
        const tempNew = [];

        while (i < oldLines.length && j < newLines.length && oldLines[i] !== newLines[j]) {
            tempOld.push(oldLines[i]);
            tempNew.push(newLines[j]);
            i++;
            j++;
            hasChanges = true;
        }

        if (hasChanges) {
            // Gelöschte Zeilen
            tempOld.forEach(line => {
                block.lines.push({ type: 'removed', content: line });
                block.oldCount++;
            });

            // Hinzugefügte Zeilen
            tempNew.forEach(line => {
                block.lines.push({ type: 'added', content: line });
                block.newCount++;
            });
        }

        // Restliche Zeilen
        while (i < oldLines.length && (j >= newLines.length || oldLines[i] !== newLines[j])) {
            block.lines.push({ type: 'removed', content: oldLines[i] });
            i++;
            block.oldCount++;
        }

        while (j < newLines.length && (i >= oldLines.length || oldLines[i] !== newLines[j])) {
            block.lines.push({ type: 'added', content: newLines[j] });
            j++;
            block.newCount++;
        }

        if (block.lines.length > 0) {
            blocks.push(block);
        }
    }

    return blocks;
}

// Empty State anzeigen
function showEmptyState(message) {
    const emptyState = document.getElementById('emptyState');
    const fileList = document.getElementById('fileList');
    const diffViewer = document.getElementById('diffViewer');

    emptyState.textContent = message;
    emptyState.style.display = 'flex';
    fileList.style.display = 'none';
    diffViewer.style.display = 'none';
}

// Buttons aktualisieren

// Buttons aktualisieren
function updateButtons() {
    const reposWithChanges = allRepos.filter(r => r.changesCount > 0);
    const allHaveMessages = reposWithChanges.every(repo => {
        const key = `${repo.category}/${repo.repo}`;
        return commitMessages[key] && commitMessages[key].trim().length > 0;
    });

    const generateBtn = document.getElementById('generateBtn');
    const commitBtn = document.getElementById('commitBtn');

    generateBtn.disabled = reposWithChanges.length === 0;
    commitBtn.disabled = !allHaveMessages || reposWithChanges.length === 0;
}