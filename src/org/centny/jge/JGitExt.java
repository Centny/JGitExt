package org.centny.jge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;

public class JGitExt {
	public static Git clone(File dir, String uri)
			throws InvalidRemoteException, TransportException, GitAPIException {
		Git re = null;
		CloneCommand command = Git.cloneRepository();
		command.setDirectory(dir);
		command.setURI(uri);
		re = command.call();
		return re;
	}

	public static Git clone(File dir, String uri, String branch)
			throws InvalidRemoteException, TransportException, GitAPIException {
		Git re = null;
		CloneCommand command = Git.cloneRepository();
		command.setBranch("refs/heads/" + branch);
		command.setDirectory(dir);
		command.setURI(uri);
		re = command.call();
		return re;
	}

	public static void addRemote(Repository repo, String name, String remote)
			throws Exception {
		File dir = repo.getDirectory();
		File cfg = new File(dir, "config");
		InputStreamReader is = new InputStreamReader(new FileInputStream(cfg),
				"UTF-8");
		BufferedReader reader = new BufferedReader(is);
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.trim().matches("\\[\\b*remote\\b*.*\\]")) {
				continue;
			}
			if (line.indexOf(name) > -1) {
				reader.close();
				throw new Exception("the remote name " + name
						+ " is aleady added");
			}
		}
		reader.close();
		OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(
				cfg, true));
		BufferedWriter writer = new BufferedWriter(os);
		writer.append("[remote \"" + name + "\"]\n");
		writer.append("\turl = " + remote + "\n");
		writer.append("\tfetch = +refs/heads/*:refs/remotes/am/*\n");
		writer.close();
	}

	public static List<DiffEntry> filter(List<DiffEntry> src, ChangeType ctype) {
		List<DiffEntry> dst = new ArrayList<DiffEntry>();
		for (DiffEntry de : src) {
			if (ctype.equals(de.getChangeType())) {
				dst.add(de);
			}
		}
		return dst;
	}
}
