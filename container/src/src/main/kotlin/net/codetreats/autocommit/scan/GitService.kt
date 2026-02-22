package net.codetreats.autocommit.scan

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

class GitService(private val gitUser: String, private val gitMail: String, private val gitHubToken: String) {

    fun openRepository(repoPath: File): Git = Git.open(repoPath).also { git ->
            setConfig(git, gitUser, gitMail)
    }

    fun getFileContent(git: Git, filePath: String, ref: String = "HEAD"): String? {
        return try {
            val repository: Repository = git.repository
            val objectId: ObjectId = repository.resolve(ref) ?: return null

            val revWalk = RevWalk(repository)
            val commit = revWalk.parseCommit(objectId)
            val treeWalk = TreeWalk.forPath(repository, filePath, commit.tree)

            if (treeWalk != null) {
                val objectLoader = repository.open(treeWalk.getObjectId(0))
                String(objectLoader.bytes, StandardCharsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getDiff(git: Git): String {
        val repository = git.repository
        val outputStream = ByteArrayOutputStream()
        val formatter = DiffFormatter(outputStream)
        formatter.setRepository(repository)

        formatter.isDetectRenames = false

        val head = repository.resolve("HEAD") ?: return ""

        val revWalk = RevWalk(repository)
        val commit = revWalk.parseCommit(head)
        val oldTreeIter = CanonicalTreeParser()
        val reader = repository.newObjectReader()
        oldTreeIter.reset(reader, commit.tree)

        val newTreeIter = FileTreeIterator(repository)

        val diffs = formatter.scan(oldTreeIter, newTreeIter)
        diffs.forEach { diffEntry ->
            formatter.format(diffEntry)
        }

        formatter.close()
        reader.close()
        revWalk.close()

        return outputStream.toString(StandardCharsets.UTF_8)
    }

    fun addAll(git: Git) {
        git.add()
            .addFilepattern(".")
            .call()
    }

    fun commit(git: Git, message: String) {
        git.commit()
            .setMessage(message)
            .call()
    }

    fun push(git: Git) {
        val credentialsProvider = UsernamePasswordCredentialsProvider("git", gitHubToken)
        git.push()
            .setCredentialsProvider(credentialsProvider)
            .call()
    }

    private fun setConfig(git: Git, name: String, email: String) {
        val config = git.repository.config
        config.setString("user", null, "name", name)
        config.setString("user", null, "email", email)
        config.setString("core", null, "autocrlf", "false")
        config.setString("core", null, "eol", "lf")
        config.save()
    }

}

