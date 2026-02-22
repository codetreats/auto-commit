# Auto-Commit Repository Scanner

A TypeScript-based tool that scans Git repositories, detects changes, and generates detailed JSON reports with file diffs.

## Overview

This tool recursively scans a directory structure containing Git repositories, analyzes their status, and generates comprehensive JSON reports for each repository. It's designed to help track changes across multiple repositories in an organized manner.

## Features

- **Multi-Repository Scanning**: Scans all Git repositories organized in category folders
- **Change Detection**: Identifies modified, added, and deleted files
- **Diff Generation**: Extracts and stores file contents before and after changes
- **Binary File Handling**: Automatically skips binary files to avoid processing issues
- **Status Tracking**: Detects uncommitted changes, unpushed commits, and up-to-date repositories
- **JSON Reports**: Generates detailed JSON files for each repository with all change information
- **Summary Generation**: Creates an overview summary of all scanned repositories

## Directory Structure

```
/git/                          # Base directory for all repositories
  ├── category1/               # Category folder
  │   ├── repo1/              # Individual repository
  │   └── repo2/
  └── category2/
      └── repo3/

/var/www/html/diffs/           # Output directory
  ├── category1_repo1.json     # Repository status files
  ├── category2_repo3.json
  ├── summary.json             # Overall summary
  ├── uuid1.txt                # Old file content
  └── uuid2.txt                # New file content
```

## Installation

```bash
npm install
```

## Usage

```bash
npm start /path/to/status/file.txt
```

The status file path is used for logging scan progress.

## Output Format

### Repository Status JSON

Each repository generates a JSON file with the following structure:

#### Example 1: Repository with Changes

```json
{
  "status": "CHANGED",
  "category": "codetreats",
  "repo": "auto-commit",
  "path": "/git/codetreats/auto-commit",
  "timestamp": "2024-01-15T14:30:45.123Z",
  "changes": [
    {
      "path": "src/main.ts",
      "type": "modified",
      "oldFile": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.txt",
      "newFile": "b2c3d4e5-f6a7-8901-bcde-f12345678901.txt"
    },
    {
      "path": "src/utils/helper.ts",
      "type": "added",
      "newFile": "c3d4e5f6-a7b8-9012-cdef-123456789012.txt"
    },
    {
      "path": "old-file.ts",
      "type": "deleted",
      "oldFile": "d4e5f6a7-b8c9-0123-def1-234567890123.txt"
    }
  ]
}
```

#### Example 2: Up-to-Date Repository

```json
{
  "status": "UPTODATE",
  "category": "others",
  "repo": "sync-it",
  "path": "/git/others/sync-it",
  "timestamp": "2024-01-15T14:30:47.456Z",
  "changes": []
}
```

#### Example 3: Repository with Unpushed Commits

```json
{
  "status": "UNPUSHED",
  "category": "codetreats",
  "repo": "sevdesk-kotlin",
  "path": "/git/codetreats/sevdesk-kotlin",
  "timestamp": "2024-01-15T14:30:48.789Z",
  "changes": []
}
```

#### Example 4: Untracked Directory (Not a Git Repository)

```json
{
  "status": "UNTRACKED",
  "category": "projects",
  "repo": "legacy-code",
  "path": "/git/projects/legacy-code",
  "timestamp": "2024-01-15T14:30:49.012Z",
  "changes": []
}
```

### Summary JSON

The `summary.json` file provides an overview of all scanned repositories:

```json
[
  {
    "category": "codetreats",
    "repo": "auto-commit",
    "status": "CHANGED",
    "changesCount": 3,
    "jsonFile": "codetreats_auto-commit.json"
  },
  {
    "category": "others",
    "repo": "sync-orders",
    "status": "UPTODATE",
    "changesCount": 0,
    "jsonFile": "others_sync-it.json"
  },
  {
    "category": "codetreats",
    "repo": "sevdesk-proxy",
    "status": "UNPUSHED",
    "changesCount": 0,
    "jsonFile": "codetreats_sevdesk-kotlin.json"
  },
  {
    "category": "projects",
    "repo": "legacy-code",
    "status": "UNTRACKED",
    "changesCount": 0,
    "jsonFile": "projects_legacy-code.json"
  }
]
```

## Status Types

- **UPTODATE**: Repository has no uncommitted changes and no unpushed commits
- **CHANGED**: Repository has uncommitted changes (modified, added, or deleted files)
- **UNPUSHED**: Repository has committed changes that haven't been pushed to origin
- **UNTRACKED**: Directory exists but is not a Git repository

## Change Types

- **modified**: File has been changed
- **added**: New file has been created
- **deleted**: File has been removed

## File Content Storage

When changes are detected, the tool stores file contents in separate UUID-named text files:

- `oldFile`: Contains the content from the last commit (HEAD)
- `newFile`: Contains the current working directory content

This allows for easy diff generation and change review without storing large amounts of data in the JSON files.

## Error Handling

The tool implements comprehensive error handling:

- Binary files are automatically skipped
- Missing remote repositories trigger warnings but don't stop execution
- Fatal errors during repository analysis cause immediate exit with error code 1
- All errors are logged with detailed context information

## Logging

The tool provides detailed console output:

```
Starting repository scan...

Scanning category: codetreats
[STEP]
[STEP] #############################################
[STEP] 2024-01-15_14:30:45: codetreats
[STEP] #############################################
  Analyzing: codetreats/auto-commit
    Status: CHANGED (3 changes)
  Analyzing: codetreats/sevdesk-proxy
    Status: UPTODATE (0 changes)

✅ Repository scan completed successfully!
   Total repositories: 15
   With changes: 3
```

## Use Cases

- **Automated Change Tracking**: Monitor multiple repositories for uncommitted changes
- **CI/CD Integration**: Detect repositories that need attention before deployment
- **Code Review Preparation**: Generate comprehensive change reports for review
- **Repository Health Monitoring**: Track the status of all repositories in your organization
- **Backup Verification**: Ensure all changes are committed and pushed

## Technical Details

- Built with TypeScript
- Uses `simple-git` for Git operations
- Uses `fs-extra` for enhanced file system operations
- Generates UUID v4 for unique file identification
- Handles both text and binary files appropriately

## License

MIT